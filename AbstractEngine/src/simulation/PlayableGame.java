package simulation;

import engine.core.GameMaster;
import engine.entities.Player;
import engine.entities.NPC;
import engine.entities.TextureObject;
import engine.entities.Entity;
import engine.managers.*;
import engine.input.InputAction;
import engine.collision.ICollisionListener;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Nutrition Game - Collect good food, answer quiz on bad food touch.
 * Features: good/bad food items, quiz with 30s countdown, round backgrounds, powerups.
 */
public class PlayableGame extends JPanel implements KeyListener, Runnable {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final String TITLE = "Nutrition Quest";
    
    // Engine components
    private GameMaster gameMaster;
    private SceneManager sceneManager;
    private EntityManager entityManager;
    private InputOutputManager inputManager;
    private MovementManager movementManager;
    private CollisionManager collisionManager;
    private TimeManager timeManager;
    private Player player;
    
    // Scenes
    private engine.scene.MenuScene menuScene;
    private engine.scene.MainScene mainScene;
    private engine.scene.EndScene endScene;
    
    // Game objects
    private java.util.List<FoodItem> goodFoods;
    private java.util.List<FoodItem> badFoods;
    private java.util.List<TextureObject> objects;
    
    // Game state
    private boolean running;
    private boolean paused;
    private Thread gameThread;
    
    // Stats
    private int score = 0;
    private int highScore = 0;
    private float gameTime = 0;
    private long lastTime;
    private int fps = 0;
    private int frameCount = 0;
    private long fpsTimer = 0;
    
    // Round & background (0-4 = different biomes)
    private int currentRound = 0;
    private static final Color[][] ROUND_BACKGROUNDS = {
        { new Color(30, 50, 30), new Color(40, 70, 40) },   // Forest green
        { new Color(40, 50, 70), new Color(60, 80, 110) },  // Beach blue
        { new Color(50, 40, 35), new Color(80, 60, 50) },   // Mountain brown
        { new Color(25, 45, 55), new Color(45, 75, 90) },   // River teal
        { new Color(45, 35, 55), new Color(70, 55, 85) }    // Sunset purple
    };
    
    // Bad food base speed, increases after each quiz
    private float badFoodBaseSpeed = 80f;
    private static final float BAD_FOOD_SPEED_INCREMENT = 25f;
    
    // Powerups
    private int powerupsRedeemed = 0;
    private static final int[] POWERUP_THRESHOLDS = { 10, 25, 50, 100 };
    private boolean powerupAvailable = false;
    private String activePowerup = null;
    private float powerupTimer = 0f;
    private boolean shieldActive = false;      // Ignore next bad food
    private boolean freezeActive = false;      // Freeze bad food for 5s
    private boolean doublePointsActive = false; // 2x points for 10s
    
    // Quiz data: { question, opt1, opt2, opt3, opt4, correctIndex (0-3) }
    private static final String[][] QUIZ_QUESTIONS = {
        { "Which vitamin is abundant in oranges?", "Vitamin A", "Vitamin C", "Vitamin D", "Vitamin K", "1" },
        { "Which food is a good source of protein?", "Candy", "Chicken", "Soda", "Chips", "1" },
        { "What is a healthy breakfast choice?", "Donut", "Oatmeal", "Fries", "Cookie", "1" },
        { "Which helps build strong bones?", "Sugar", "Calcium", "Salt", "Oil", "1" },
        { "Which is a whole grain?", "White bread", "Brown rice", "Cake", "Candy", "1" },
        { "What should you drink most of?", "Soda", "Energy drink", "Water", "Milkshake", "2" },
        { "Which food is high in fiber?", "Ice cream", "Broccoli", "Candy", "Chips", "1" },
        { "What nutrient do carrots provide?", "Protein", "Vitamin A", "Fat", "Sugar", "1" },
        { "Which is a healthy snack?", "Apple", "Cookie", "Candy bar", "Cake", "0" },
        { "What does iron help with?", "Taste", "Blood health", "Smell", "Hair color", "1" }
    };
    private int quizIndex = 0;
    
    // Menu
    private int menuSelection = 0;
    private String[] menuOptions = {"Start Game", "Instructions", "Exit"};
    private long lastKeyPress = 0;
    private static final long KEY_COOLDOWN = 200;

    public PlayableGame() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        
        initializeEngine();
        initializeScenes();
    }
    
    private void initializeEngine() {
        gameMaster = new GameMaster();
        gameMaster.initialize();
        sceneManager = gameMaster.getSceneManager();
        entityManager = gameMaster.getEntityManager();
        inputManager = gameMaster.getInputOutputManager();
        movementManager = gameMaster.getMovementManager();
        collisionManager = gameMaster.getCollisionManager();
        timeManager = gameMaster.getTimeManager();
        
        inputManager.bindAction(InputAction.CONFIRM, KeyEvent.VK_ENTER);
        inputManager.bindAction(InputAction.ACTION_2, KeyEvent.VK_Q);
        
        initializeSounds();
    }
    
    private void initializeScenes() {
        menuScene = new engine.scene.MenuScene(inputManager);
        mainScene = new engine.scene.MainScene(entityManager, movementManager, collisionManager, inputManager);
        endScene = new engine.scene.EndScene(inputManager);
        
        sceneManager.addScene("MenuScene", menuScene);
        sceneManager.addScene("MainScene", mainScene);
        sceneManager.addScene("EndScene", endScene);
        
        sceneManager.loadScene("MenuScene");
    }
    
    private void initializeSounds() {
        // Sounds will use beep fallback
        System.out.println("Sounds loaded (using beep)");
    }
    
    private void initializeGame() {
        entityManager.clear();
        collisionManager.clearAll();
        mainScene.reset();
        timeManager.reset();
        paused = false;
        score = 0;
        gameTime = 0;
        currentRound = 0;
        badFoodBaseSpeed = 80f;
        powerupAvailable = false;
        powerupsRedeemed = 0;
        activePowerup = null;
        shieldActive = false;
        freezeActive = false;
        doublePointsActive = false;
        
        if (goodFoods == null) goodFoods = new java.util.ArrayList<>();
        if (badFoods == null) badFoods = new java.util.ArrayList<>();
        if (objects == null) objects = new java.util.ArrayList<>();
        goodFoods.clear();
        badFoods.clear();
        objects.clear();
        
        float playerX = WINDOW_WIDTH / 2f;
        float playerY = WINDOW_HEIGHT / 2f;
        player = new Player(playerX, playerY, "Player");
        player.setSpeed(200.0f);
        player.setWidth(32);
        player.setHeight(32);
        entityManager.addEntity(player);
        movementManager.addEntity(player);
        collisionManager.addCollidable(player, "player");
        
        mainScene.setPlayer(player);
        
        java.util.Random rnd = new java.util.Random();
        int padding = 60;
        float safeRadius = 100f;
        
        // Good food items (green) - collect for points
        String[] goodNames = {"Apple", "Banana", "Broccoli", "Carrot", "Orange", "Grapes", "Spinach", "Tomato"};
        String[] goodBehaviors = {"wander", "wander", "patrol", "wander", "patrol", "wander", "patrol", "wander"};
        for (int i = 0; i < goodNames.length; i++) {
            float x, y;
            do {
                x = padding + rnd.nextFloat() * (WINDOW_WIDTH  - 2 * padding);
                y = padding + rnd.nextFloat() * (WINDOW_HEIGHT - 2 * padding);
            } while (dist(x, y, playerX, playerY) < safeRadius);
            createGoodFood(x, y, goodBehaviors[i], goodNames[i]);
        }
        
        // Bad food items (red) - touch triggers quiz
        String[] badNames = {"Burger", "Soda", "Chips", "Candy", "Pizza"};
        String[] badBehaviors = {"patrol", "wander", "patrol", "wander", "patrol"};
        for (int i = 0; i < badNames.length; i++) {
            float x, y;
            do {
                x = padding + rnd.nextFloat() * (WINDOW_WIDTH  - 2 * padding);
                y = padding + rnd.nextFloat() * (WINDOW_HEIGHT - 2 * padding);
            } while (dist(x, y, playerX, playerY) < safeRadius);
            createBadFood(x, y, badBehaviors[i], badNames[i]);
        }
        
        // Obstacles
        int[][] sizes = {{60,60},{80,40},{50,50},{70,70},{55,55},{75,45}};
        for (int[] sz : sizes) {
            float x, y;
            do {
                x = padding + rnd.nextFloat() * (WINDOW_WIDTH  - 2 * padding);
                y = padding + rnd.nextFloat() * (WINDOW_HEIGHT - 2 * padding);
            } while (dist(x, y, playerX, playerY) < safeRadius);
            createObstacle(x, y, sz[0], sz[1]);
        }
        
        setupCollisions();
        
        System.out.println("\n=== Nutrition Quest Started ===");
    }
    
    /** Euclidean distance helper */
    private float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    private void createGoodFood(float x, float y, String behavior, String name) {
        FoodItem f = new FoodItem(x, y, behavior, name, true);
        f.setWidth(32);
        f.setHeight(32);
        f.setSpeed(60f);
        goodFoods.add(f);
        entityManager.addEntity(f);
        collisionManager.addCollidable(f, "good_food");
    }
    
    private void createBadFood(float x, float y, String behavior, String name) {
        FoodItem f = new FoodItem(x, y, behavior, name, false);
        f.setWidth(32);
        f.setHeight(32);
        f.setSpeed(badFoodBaseSpeed);
        badFoods.add(f);
        entityManager.addEntity(f);
        collisionManager.addCollidable(f, "bad_food");
    }
    
    private void createObstacle(float x, float y, float w, float h) {
        TextureObject obj = new TextureObject("obstacle", x, y, 1.0f);
        obj.setWidth(w);
        obj.setHeight(h);
        objects.add(obj);
        entityManager.addEntity(obj);
        collisionManager.addCollidable(obj, "wall");
    }
    
    private void setupCollisions() {
        // Good food: collect for points
        collisionManager.registerHandler("player", "good_food", new ICollisionListener() {
            @Override
            public void onCollision(Entity a, Entity b) {
                if (!(b instanceof FoodItem)) return;
                FoodItem f = (FoodItem) b;
                if (!f.isGoodFood() || !f.isActive()) return;
                int pts = doublePointsActive ? 2 : 1;
                score += pts;
                inputManager.getSpeaker().beep();
                f.setActive(false);
                entityManager.removeEntity(f);
                collisionManager.removeCollidable(f);
                goodFoods.remove(f);
            }
        });
        
        // Bad food: show quiz, +1 if correct, increase speed after
        collisionManager.registerHandler("player", "bad_food", new ICollisionListener() {
            @Override
            public void onCollision(Entity a, Entity b) {
                if (!(b instanceof FoodItem)) return;
                FoodItem f = (FoodItem) b;
                if (!f.isGoodFood() && f.isActive()) {
                    if (shieldActive) {
                        shieldActive = false;
                        inputManager.getSpeaker().beep();
                        return;
                    }
                    handleBadFoodTouch(f);
                }
            }
        });
        
        // Generic wall collision handler - works for any entity type
        ICollisionListener wallCollisionHandler = new ICollisionListener() {
            private long lastWallSoundTime = 0;
            private static final long WALL_SOUND_COOLDOWN = 200; // Prevent sound spam
            
            @Override
            public void onCollision(Entity entity, Entity wall) {
                // Calculate overlap and push entity out of wall
                float dx = entity.getX() - wall.getX();
                float dy = entity.getY() - wall.getY();
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                
                if (dist > 0) {
                    // Calculate overlap amount
                    float overlapX = (entity.getWidth() / 2 + wall.getWidth() / 2) - Math.abs(dx);
                    float overlapY = (entity.getHeight() / 2 + wall.getHeight() / 2) - Math.abs(dy);
                    
                    // Push entity out along the axis with smallest overlap
                    if (overlapX < overlapY) {
                        float pushX = (dx / dist) * overlapX;
                        entity.setPosition(entity.getX() + pushX, entity.getY());
                    } else {
                        float pushY = (dy / dist) * overlapY;
                        entity.setPosition(entity.getX(), entity.getY() + pushY);
                    }
                    
                    String entityLayer = collisionManager.getEntityLayer(entity);
                    if ("player".equals(entityLayer)) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastWallSoundTime > WALL_SOUND_COOLDOWN) {
                            inputManager.getSpeaker().beep();
                            lastWallSoundTime = currentTime;
                        }
                    }
                }
            }
        };
        
        // Register wall collision for player
        collisionManager.registerHandler("player", "wall", wallCollisionHandler);
        
        // Food vs wall - use same silent handler via generic listener below
        
        // Register wall collision for any other entity type (abstract/generic)
        // This allows any entity layer to collide with walls without hardcoding
        collisionManager.addListener(new ICollisionListener() {
            @Override
            public void onCollision(Entity a, Entity b) {
                String layerA = collisionManager.getEntityLayer(a);
                String layerB = collisionManager.getEntityLayer(b);
                
                // If one entity is a wall and the other is not a player, handle silently (no sound)
                if ("wall".equals(layerA) && !"wall".equals(layerB) && !"player".equals(layerB)) {
                    handleSilentWallCollision(b, a);
                } else if ("wall".equals(layerB) && !"wall".equals(layerA) && !"player".equals(layerA)) {
                    handleSilentWallCollision(a, b);
                }
            }
        });
    }
    
    /**
     * Handle wall collision silently (for NPCs) - no sound
     */
    private void handleSilentWallCollision(Entity entity, Entity wall) {
        // Calculate overlap and push entity out of wall
        float dx = entity.getX() - wall.getX();
        float dy = entity.getY() - wall.getY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (dist > 0) {
            // Calculate overlap amount
            float overlapX = (entity.getWidth() / 2 + wall.getWidth() / 2) - Math.abs(dx);
            float overlapY = (entity.getHeight() / 2 + wall.getHeight() / 2) - Math.abs(dy);
            
            // Push entity out along the axis with smallest overlap
            if (overlapX < overlapY) {
                float pushX = (dx / dist) * overlapX;
                entity.setPosition(entity.getX() + pushX, entity.getY());
            } else {
                float pushY = (dy / dist) * overlapY;
                entity.setPosition(entity.getX(), entity.getY() + pushY);
            }
        }
    }
    
    private void showPowerupDialog() {
        String[] options = { "Freeze (stop bad food 5s)", "Shield (ignore 1 bad food)", "Double Points (10s)" };
        int choice = JOptionPane.showOptionDialog(this, "Choose a powerup!", "Powerup!",
            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        powerupAvailable = false;
        powerupsRedeemed++;
        activePowerup = choice == 0 ? "freeze" : (choice == 1 ? "shield" : "double");
        powerupTimer = choice == 0 ? 5f : (choice == 1 ? 0f : 10f);
        if (choice == 0) freezeActive = true;
        if (choice == 1) shieldActive = true;
        if (choice == 2) doublePointsActive = true;
        inputManager.getSpeaker().beep();
    }
    
    /** Handle bad food touch: show quiz, award point if correct, increase speed, respawn bad food */
    private void handleBadFoodTouch(FoodItem f) {
        f.setActive(false);
        entityManager.removeEntity(f);
        collisionManager.removeCollidable(f);
        badFoods.remove(f);
        
        String[] q = QUIZ_QUESTIONS[quizIndex % QUIZ_QUESTIONS.length];
        quizIndex++;
        String question = q[0];
        String[] options = { q[1], q[2], q[3], q[4] };
        int correctIdx = Integer.parseInt(q[5]);
        
        Frame frame = null;
        if (SwingUtilities.getWindowAncestor(this) instanceof Frame) {
            frame = (Frame) SwingUtilities.getWindowAncestor(this);
        }
        if (frame == null && Frame.getFrames().length > 0) {
            frame = Frame.getFrames()[0];
        }
        
        final Frame parentFrame = frame;
        final String quizQuestion = question;
        final String[] quizOpts = options;
        final int quizCorrect = correctIdx;
        final boolean[] result = { false };
        try {
            if (parentFrame != null) {
                SwingUtilities.invokeAndWait(() -> {
                    result[0] = QuizDialog.showQuiz(parentFrame, quizQuestion, quizOpts, quizCorrect);
                });
            } else {
                String answer = (String) JOptionPane.showInputDialog(this, quizQuestion, "Quiz",
                    JOptionPane.QUESTION_MESSAGE, null, quizOpts, quizOpts[quizCorrect]);
                result[0] = answer != null && answer.equals(quizOpts[quizCorrect]);
            }
        } catch (InterruptedException | InvocationTargetException ex) {
            result[0] = false;
        }
        
        if (result[0]) {
            score++;
            inputManager.getSpeaker().beep();
        }
        
        badFoodBaseSpeed += BAD_FOOD_SPEED_INCREMENT;
        for (FoodItem bf : new java.util.ArrayList<>(badFoods)) {
            bf.setSpeed(badFoodBaseSpeed);
        }
        
        java.util.Random rnd = new java.util.Random();
        int padding = 60;
        float x = padding + rnd.nextFloat() * (WINDOW_WIDTH - 2 * padding);
        float y = padding + rnd.nextFloat() * (WINDOW_HEIGHT - 2 * padding);
        String[] badNames = {"Burger", "Soda", "Chips", "Candy", "Pizza"};
        String[] badBehaviors = {"patrol", "wander", "patrol", "wander", "patrol"};
        int idx = rnd.nextInt(badNames.length);
        createBadFood(x, y, badBehaviors[idx], badNames[idx]);
    }
    
    public void start() {
        if (running) return;
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        lastTime = System.nanoTime();
        fpsTimer = System.currentTimeMillis();
        
        while (running) {
            long currentTime = System.nanoTime();
            float rawDt = (currentTime - lastTime) / 1_000_000_000.0f;
            lastTime = currentTime;
            
            timeManager.update(rawDt);
            float dt = timeManager.getDeltaTime();
            
            inputManager.pollInput();
            sceneManager.update(dt);
            inputManager.processOutput();
            
            updateGameState();
            checkSceneTransitions();
            
            repaint();
            
            frameCount++;
            if (System.currentTimeMillis() - fpsTimer >= 1000) {
                fps = frameCount;
                frameCount = 0;
                fpsTimer = System.currentTimeMillis();
            }
            
            try { Thread.sleep(16); } catch (InterruptedException e) { }
        }
    }
    
    private void checkSceneTransitions() {
        engine.scene.Scene current = sceneManager.getActiveScene();
        
        if (current != null && "MenuScene".equals(current.getSceneName()) && menuScene.isTransitionRequested()) {
            menuScene.resetTransition();
            sceneManager.loadScene("MainScene");
            initializeGame();
        } else if (current != null && "MainScene".equals(current.getSceneName()) && mainScene.isGameOver()) {
            gameTime = timeManager.getTotalTime();
            score = (int)(gameTime * 10);
            if (score > highScore) highScore = score;
            endScene.setResults(score, gameTime);
            sceneManager.loadScene("EndScene");
        } else if (current != null && "EndScene".equals(current.getSceneName())) {
            if (endScene.isRestartRequested()) {
                endScene.resetRequests();
                sceneManager.loadScene("MainScene");
                initializeGame();
            } else if (endScene.isExitRequested()) {
                endScene.resetRequests();
                sceneManager.loadScene("MenuScene");
            }
        }
    }
    
    private void updateGameState() {
        engine.scene.Scene current = sceneManager.getActiveScene();
        if (current != null && "MainScene".equals(current.getSceneName()) && !mainScene.isGameOver()) {
            gameTime = timeManager.getTotalTime();
            currentRound = Math.min(score / 10, ROUND_BACKGROUNDS.length - 1);
            keepPlayerInBounds();
            
            if (freezeActive) {
                for (FoodItem bf : badFoods) bf.setSpeed(0);
            } else {
                for (FoodItem bf : badFoods) bf.setSpeed(badFoodBaseSpeed);
            }
            if (powerupTimer > 0) {
                powerupTimer -= timeManager.getDeltaTime();
                if (powerupTimer <= 0) {
                    if ("freeze".equals(activePowerup)) freezeActive = false;
                    if ("double".equals(activePowerup)) doublePointsActive = false;
                    activePowerup = null;
                }
            }
            
            if (powerupsRedeemed < POWERUP_THRESHOLDS.length && score >= POWERUP_THRESHOLDS[powerupsRedeemed]) {
                powerupAvailable = true;
            }
            
            if (score >= 50) {
                mainScene.setGameOver(true);
            }
        }
    }
    
    private void keepPlayerInBounds() {
        float hw = player.getWidth() / 2;
        float hh = player.getHeight() / 2;
        if (player.getX() < hw) player.setX(hw);
        if (player.getX() > WINDOW_WIDTH - hw) player.setX(WINDOW_WIDTH - hw);
        if (player.getY() < hh) player.setY(hh);
        if (player.getY() > WINDOW_HEIGHT - hh) player.setY(WINDOW_HEIGHT - hh);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        engine.scene.Scene current = sceneManager.getActiveScene();
        if (current != null) {
            String sceneName = current.getSceneName();
            if ("MenuScene".equals(sceneName)) {
                drawMenu(g2d);
            } else if ("MainScene".equals(sceneName)) {
                drawGame(g2d);
                if (paused) {
                    drawPaused(g2d);
                }
            } else if ("EndScene".equals(sceneName)) {
                drawGame(g2d);
                drawGameOver(g2d);
            }
        } else {
            drawMenu(g2d);
        }
    }
    
    private void drawMenu(Graphics2D g2d) {
        GradientPaint gp = new GradientPaint(0, 0, new Color(20, 20, 50), 0, WINDOW_HEIGHT, new Color(50, 20, 50));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        drawCentered(g2d, "NUTRITION QUEST", 150);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        drawCentered(g2d, "Collect Good Food, Learn from Bad!", 190);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        for (int i = 0; i < menuOptions.length; i++) {
            if (i == menuSelection) {
                g2d.setColor(Color.YELLOW);
                g2d.fillRect(WINDOW_WIDTH / 2 - 120, 280 + i * 60, 240, 45);
                g2d.setColor(Color.BLACK);
            } else {
                g2d.setColor(Color.WHITE);
            }
            drawCentered(g2d, menuOptions[i], 315 + i * 60);
        }
        
        if (highScore > 0) {
            g2d.setColor(Color.CYAN);
            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            drawCentered(g2d, "High Score: " + highScore, 520);
        }
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        drawCentered(g2d, "Use ↑↓ to select, ENTER to confirm", 560);
    }
    
    private void drawGame(Graphics2D g2d) {
        Color[] bg = ROUND_BACKGROUNDS[Math.min(currentRound, ROUND_BACKGROUNDS.length - 1)];
        GradientPaint gp = new GradientPaint(0, 0, bg[0], WINDOW_WIDTH, WINDOW_HEIGHT, bg[1]);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        g2d.setColor(new Color(0, 0, 0, 40));
        for (int x = 0; x < WINDOW_WIDTH; x += 50) g2d.drawLine(x, 0, x, WINDOW_HEIGHT);
        for (int y = 0; y < WINDOW_HEIGHT; y += 50) g2d.drawLine(0, y, WINDOW_WIDTH, y);
        
        if (objects != null) {
            for (TextureObject obj : objects) {
                if (obj != null && obj.isActive()) {
                    drawEntity(g2d, obj, new Color(100, 100, 100));
                }
            }
        }
        
        if (goodFoods != null) {
            for (FoodItem f : goodFoods) {
                if (f != null && f.isActive()) {
                    drawFoodItem(g2d, f, true);
                }
            }
        }
        
        if (badFoods != null) {
            for (FoodItem f : badFoods) {
                if (f != null && f.isActive()) {
                    drawFoodItem(g2d, f, true);
                }
            }
        }
        
        if (player != null && player.isActive()) {
            drawEntity(g2d, player, new Color(70, 130, 200));
        }
        
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WINDOW_WIDTH, 55);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Score: " + score, 20, 32);
        g2d.drawString("Round: " + (currentRound + 1), 150, 32);
        g2d.drawString("Time: " + String.format("%.1f", gameTime) + "s", 280, 32);
        g2d.drawString("ESC: Pause", WINDOW_WIDTH - 150, 32);
        if (powerupAvailable) {
            g2d.setColor(Color.YELLOW);
            g2d.drawString("Press P for Powerup!", WINDOW_WIDTH / 2 - 80, 32);
        }
        if (activePowerup != null && powerupTimer > 0) {
            g2d.setColor(Color.CYAN);
            g2d.drawString(activePowerup.toUpperCase() + ": " + (int) powerupTimer + "s", 450, 32);
        }
    }
    
    private void drawFoodItem(Graphics2D g2d, FoodItem f, boolean useImage) {
        if (!f.isActive()) return;
        int x = (int)(f.getX() - f.getWidth() / 2);
        int y = (int)(f.getY() - f.getHeight() / 2);
        int w = (int)f.getWidth(), h = (int)f.getHeight();
        
        Image img = useImage ? FoodImageLoader.getFoodImage(f.getFoodName()) : null;
        if (img != null) {
            g2d.drawImage(img, x, y, w, h, null);
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRoundRect(x, y, w, h, 4, 4);
        } else {
            Color fill = f.isGoodFood() ? new Color(50, 180, 80) : new Color(220, 80, 80);
            g2d.setColor(fill);
            g2d.fillOval(x, y, w, h);
            g2d.setColor(fill.darker());
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x, y, w, h);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            String name = f.getFoodName();
            int tw = g2d.getFontMetrics().stringWidth(name);
            g2d.drawString(name, x + (w - tw) / 2, y + h / 2 + 4);
        }
    }
    
    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        int bx = 150, by = 125, bw = 500, bh = 350;
        g2d.setColor(new Color(60, 20, 20));
        g2d.fillRoundRect(bx, by, bw, bh, 20, 20);
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRoundRect(bx, by, bw, bh, 20, 20);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 42));
        drawCentered(g2d, "SIMULATION ENDED", by + 75);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        drawCentered(g2d, "Final Score: " + score, by + 140);
        drawCentered(g2d, "Time Survived: " + String.format("%.1f", gameTime) + "s", by + 180);
        
        if (score == highScore && score > 0) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            drawCentered(g2d, "★ NEW HIGH SCORE! ★", by + 240);
        }
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        drawCentered(g2d, "Press SPACE to Play Again", by + 280);
        drawCentered(g2d, "Press ESC for Main Menu", by + 310);
    }
    
    private void drawPaused(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        int bx = 220, by = 200, bw = 360, bh = 180;
        g2d.setColor(new Color(20, 20, 60));
        g2d.fillRoundRect(bx, by, bw, bh, 20, 20);
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRoundRect(bx, by, bw, bh, 20, 20);
        
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 48));
        drawCentered(g2d, "PAUSED", by + 65);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        drawCentered(g2d, "Press ESC to Resume", by + 115);
        drawCentered(g2d, "Press Q to Quit to Menu", by + 145);
    }
    
    private void drawEntity(Graphics2D g2d, Entity e, Color c) {
        if (!e.isActive()) return;
        int x = (int)(e.getX() - e.getWidth() / 2);
        int y = (int)(e.getY() - e.getHeight() / 2);
        int w = (int)e.getWidth(), h = (int)e.getHeight();
        
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillOval(x + 4, y + h - 8, w, 12);
        
        g2d.setColor(c);
        g2d.fillRoundRect(x, y, w, h, 8, 8);
        g2d.setColor(c.darker());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x, y, w, h, 8, 8);
        
        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.fillRoundRect(x + 4, y + 4, w / 2, h / 3, 4, 4);
    }
    
    private void drawCentered(Graphics2D g2d, String text, int y) {
        int w = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, (WINDOW_WIDTH - w) / 2, y);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        long t = System.currentTimeMillis();
        
        inputManager.getKeyboard().keyDown(k);
        if (k == KeyEvent.VK_UP) inputManager.getKeyboard().keyDown(87);
        else if (k == KeyEvent.VK_DOWN) inputManager.getKeyboard().keyDown(83);
        else if (k == KeyEvent.VK_LEFT) inputManager.getKeyboard().keyDown(65);
        else if (k == KeyEvent.VK_RIGHT) inputManager.getKeyboard().keyDown(68);
        
        engine.scene.Scene current = sceneManager.getActiveScene();
        
        if (current != null && "MenuScene".equals(current.getSceneName())) {
            if (t - lastKeyPress < KEY_COOLDOWN) return;
            if (inputManager.isPressed(InputAction.MOVE_UP)) {
                menuSelection = (menuSelection - 1 + menuOptions.length) % menuOptions.length;
                inputManager.getSpeaker().beep();
                lastKeyPress = t;
            } else if (inputManager.isPressed(InputAction.MOVE_DOWN)) {
                menuSelection = (menuSelection + 1) % menuOptions.length;
                inputManager.getSpeaker().beep();
                lastKeyPress = t;
            } else if (inputManager.isPressed(InputAction.CONFIRM)) {
                handleMenuSelection();
                lastKeyPress = t;
            }
        } else if (current != null && "MainScene".equals(current.getSceneName())) {
            if (inputManager.isPressed(InputAction.PAUSE)) {
                if (paused) {
                    paused = false;
                    timeManager.resume();
                    lastTime = System.nanoTime();
                } else {
                    paused = true;
                    timeManager.pause();
                    inputManager.getKeyboard().clearAll();
                }
            } else if (inputManager.isPressed(InputAction.ACTION_2) && paused) {
                paused = false;
                timeManager.resume();
                inputManager.getKeyboard().clearAll();
                sceneManager.loadScene("MenuScene");
            } else if (k == KeyEvent.VK_P && !paused && powerupAvailable) {
                showPowerupDialog();
            }
        } else if (current != null && "EndScene".equals(current.getSceneName())) {
            if (inputManager.isPressed(InputAction.ACTION_1)) {
                inputManager.getKeyboard().clearAll();
                endScene.resetRequests();
                entityManager.clear();
                sceneManager.loadScene("MainScene");
                initializeGame();
                lastTime = System.nanoTime();
            } else if (inputManager.isPressed(InputAction.CANCEL)) {
                inputManager.getKeyboard().clearAll();
                endScene.resetRequests();
                entityManager.clear();
                sceneManager.loadScene("MenuScene");
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        inputManager.getKeyboard().keyUp(k);
        if (k == KeyEvent.VK_UP) inputManager.getKeyboard().keyUp(87);
        else if (k == KeyEvent.VK_DOWN) inputManager.getKeyboard().keyUp(83);
        else if (k == KeyEvent.VK_LEFT) inputManager.getKeyboard().keyUp(65);
        else if (k == KeyEvent.VK_RIGHT) inputManager.getKeyboard().keyUp(68);
    }

    @Override
    public void keyTyped(KeyEvent e) { }
    
    private void handleMenuSelection() {
        switch (menuSelection) {
            case 0:
                menuScene.resetTransition();
                sceneManager.loadScene("MainScene");
                initializeGame();
                lastTime = System.nanoTime();
                break;
            case 1:
                JOptionPane.showMessageDialog(this,
                    "CONTROLS:\n• WASD or Arrow Keys - Move\n• ESC - Pause, Q - Quit to Menu\n• P - Redeem Powerup (when available)\n\n" +
                    "OBJECTIVE:\n• Collect GREEN food (good) for +1 point\n• Touch RED food (bad) to answer a quiz\n" +
                    "• Correct quiz = +1 point; wrong = no point\n• After each quiz, bad food speeds up!\n\n" +
                    "POWERUPS (at 10, 25, 50, 100 points):\n• Freeze - Stop bad food for 5 seconds\n" +
                    "• Shield - Ignore the next bad food touch\n• Double Points - 2x points for 10 seconds",
                    "How to Play", JOptionPane.INFORMATION_MESSAGE);
                break;
            case 2:
                System.exit(0);
                break;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(TITLE);
        PlayableGame game = new PlayableGame();
            
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
        frame.add(game);
        frame.pack();
            frame.setLocationRelativeTo(null);
        frame.setVisible(true);
            
            game.start();
            
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║  Nutrition Quest - Healthy Eating Game ║");
            System.out.println("║  Collect Good Food, Quiz on Bad Food!  ║");
            System.out.println("╚════════════════════════════════════════╝\n");
        });
    }
}
