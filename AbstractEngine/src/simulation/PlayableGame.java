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

/**
 * Complete Interactive Simulation with Menu, Pause, and Simulation Over screens
 */
public class PlayableGame extends JPanel implements KeyListener, Runnable {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final String TITLE = "Abstract Engine - Complete Demo";
    
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
    private java.util.List<NPC> npcs;
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
    
    // Menu
    private int menuSelection = 0;
    private String[] menuOptions = {"Start Simulation", "Instructions", "Exit"};
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
        
        if (npcs == null) npcs = new java.util.ArrayList<>();
        if (objects == null) objects = new java.util.ArrayList<>();
        npcs.clear();
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
        float safeRadius = 130f;
        
        // Spawn 15 NPCs with randomized positions (5 patrol=red, 10 wander=pink)
        String[] behaviors = {"patrol", "patrol", "patrol", "patrol", "patrol",
                              "wander", "wander", "wander", "wander", "wander",
                              "wander", "wander", "wander", "wander", "wander"};
        for (String behavior : behaviors) {
            float x, y;
            do {
                x = padding + rnd.nextFloat() * (WINDOW_WIDTH  - 2 * padding);
                y = padding + rnd.nextFloat() * (WINDOW_HEIGHT - 2 * padding);
            } while (dist(x, y, playerX, playerY) < safeRadius);
            createNPC(x, y, behavior);
        }
        
        // Spawn 10 obstacles with randomized positions and varied sizes
        int[][] sizes = {{60,60},{80,40},{50,50},{70,70},{90,35},
                         {55,55},{75,45},{65,65},{85,50},{50,70}};
        for (int[] sz : sizes) {
            float x, y;
            do {
                x = padding + rnd.nextFloat() * (WINDOW_WIDTH  - 2 * padding);
                y = padding + rnd.nextFloat() * (WINDOW_HEIGHT - 2 * padding);
            } while (dist(x, y, playerX, playerY) < safeRadius);
            createObstacle(x, y, sz[0], sz[1]);
        }
        
        setupCollisions();
        
        System.out.println("\n=== New Simulation Started ===");
    }
    
    /** Euclidean distance helper */
    private float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    private void createNPC(float x, float y, String behavior) {
        NPC npc = new NPC(x, y, behavior);
        npc.setWidth(32);
        npc.setHeight(32);
        npcs.add(npc);
        entityManager.addEntity(npc);
        collisionManager.addCollidable(npc, "enemy");
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
        collisionManager.registerHandler("player", "enemy", new ICollisionListener() {
            @Override
            public void onCollision(Entity a, Entity b) {
                inputManager.getSpeaker().beep();
                mainScene.setGameOver(true);
                System.out.println("=== SIMULATION ENDED ===");
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
        if (current != null && "MainScene".equals(current.getSceneName())) {
            gameTime = timeManager.getTotalTime();
            score = (int)(gameTime * 10);
            if (!mainScene.isGameOver()) {
                keepPlayerInBounds();
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
        drawCentered(g2d, "ABSTRACT ENGINE", 150);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        drawCentered(g2d, "Game Demo", 190);
        
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
        g2d.setColor(new Color(30, 30, 30));
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        g2d.setColor(new Color(50, 50, 50));
        for (int x = 0; x < WINDOW_WIDTH; x += 50) g2d.drawLine(x, 0, x, WINDOW_HEIGHT);
        for (int y = 0; y < WINDOW_HEIGHT; y += 50) g2d.drawLine(0, y, WINDOW_WIDTH, y);
        
        // Draw obstacles
        if (objects != null) {
            for (TextureObject obj : objects) {
                if (obj != null && obj.isActive()) {
                    drawEntity(g2d, obj, new Color(100, 100, 100));
                }
            }
        }
        
        // Draw NPCs
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc != null && npc.isActive()) {
                    Color c = npc.getBehaviorType().equals("patrol") ? new Color(255, 100, 100) : new Color(255, 150, 150);
                    drawEntity(g2d, npc, c);
                }
            }
        }
        
        // Draw player
        if (player != null && player.isActive()) {
            drawEntity(g2d, player, Color.GREEN);
        }
        
        // Draw UI overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WINDOW_WIDTH, 50);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Score: " + score, 20, 30);
        g2d.drawString("Time: " + String.format("%.1f", gameTime) + "s", 200, 30);
        g2d.drawString("FPS: " + fps, 400, 30);
        g2d.drawString("ESC: Pause", WINDOW_WIDTH - 150, 30);
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
                    "CONTROLS:\n• WASD or Arrow Keys - Move\n• ESC - Pause Game\n\n" +
                    "OBJECTIVE:\n• Avoid the red enemies\n• Navigate around gray obstacles\n" +
                    "• Survive as long as possible!\n\nSCORING:\n• +10 points per second\n" +
                    "• Simulation Over if you touch an enemy",
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
            System.out.println("║  Abstract Engine - Complete Version   ║");
            System.out.println("║  Menu → Play → Pause → Simulation Over ║");
            System.out.println("╚════════════════════════════════════════╝\n");
        });
    }
}
