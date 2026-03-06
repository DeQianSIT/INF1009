# Team Project Part 2 — Game Proposals

---

## Proposal 1: NutriQuest — Healthy Eating Adventure

### Problem Statement

Children aged 8–12 often lack awareness of basic nutrition principles, leading to poor dietary habits that carry into adulthood. Traditional educational methods such as textbooks and classroom lectures struggle to engage this age group, resulting in low retention of nutritional knowledge.

### Proposed Solution

NutriQuest is an interactive educational game that teaches children about nutrition and healthy eating through engaging gameplay. Players take on the role of a character navigating a grocery store or kitchen environment, where they must collect nutritious food items while avoiding junk food. The game reinforces learning through direct gameplay mechanics — healthy choices are rewarded, while poor choices have consequences.

### Target Audience

- Primary: Children aged 8–12
- Secondary: Parents and educators seeking interactive tools for nutrition education

### Core Gameplay

- The player controls a character that moves freely in a 2D environment.
- Food items (represented as NPCs) roam the environment with varying movement patterns.
- **Healthy foods** (fruits, vegetables, whole grains) award points when collected.
- **Unhealthy foods** (sugary snacks, fast food) slow the player down or reduce health upon contact.
- Each level represents a different meal — Breakfast, Lunch, and Dinner — with food items appropriate to that meal.
- A countdown timer adds urgency; players must collect enough healthy items before time runs out.
- Periodic quiz prompts (e.g., "Which food group does rice belong to?") award bonus time for correct answers.
- Difficulty increases across levels with faster food movement and more deceptive items.

### How It Addresses the Problem

- Players learn to identify healthy vs. unhealthy foods through repeated, gameplay-driven exposure.
- Quiz prompts reinforce food group knowledge and nutritional facts.
- The reward/penalty system builds positive associations with healthy choices.
- Progressive difficulty sustains engagement and encourages replayability.

### Engine Utilisation

| Engine Subsystem       | Application in NutriQuest                                      |
|------------------------|----------------------------------------------------------------|
| EntityManager          | Manages all food items, the player, and environmental objects  |
| MovementManager        | Handles player movement and food item motion patterns          |
| CollisionManager       | Detects food collection (player–food) and hazard contact       |
| SceneManager           | Transitions between Menu, Level Select, Gameplay, and Results  |
| TimeManager            | Countdown timer per level, difficulty pacing                   |
| InputOutputManager     | Player controls (WASD/Arrow keys), sound feedback on actions   |

### Design Patterns

1. **Factory Pattern** — A `FoodFactory` class centralises the creation of food entities. Each food item is configured with properties such as nutrition category, point value, speed, and movement type. Adding a new food item requires only a new entry in the factory — no modifications to existing code.

2. **Strategy Pattern** — Food movement behaviours are encapsulated as interchangeable strategies (e.g., `FloatStrategy`, `DashStrategy`, `CircleStrategy`). Different food types use different movement strategies, and these can be swapped at runtime to vary difficulty.

3. **Observer Pattern** — A `NutritionTracker` observes collection events and independently updates the score, health bar, and educational feedback display. This decouples the scoring logic from the collision and entity systems.

### Scalability

- Adding a new food item: Create a new configuration entry in `FoodFactory` — no changes to existing classes.
- Adding a new level/meal: Create a new `Scene` subclass with its own food spawn configuration.
- Adding a new movement behaviour: Implement the `MovementStrategy` interface — existing food items remain unaffected.
- Adding quiz content: Extend the question data set — no changes to game logic.

---

## Proposal 2: EcoSort — Sustainable Living Recycling Game

### Problem Statement

Improper waste sorting is a significant contributor to recycling contamination, with studies showing that a large percentage of recyclable materials end up in landfills due to consumer confusion. Many families are unaware of correct sorting practices, particularly for items that are commonly misclassified (e.g., pizza boxes, certain plastics, electronic waste).

### Proposed Solution

EcoSort is an interactive recycling education game that teaches players correct waste sorting through fast-paced, engaging gameplay. Players sort items arriving on a conveyor belt into the appropriate waste categories, learning through immediate feedback and reinforcement. The game presents increasingly challenging items and speeds, building both knowledge and quick decision-making skills.

### Target Audience

- Primary: Families and children aged 10+
- Secondary: Schools and community programmes promoting sustainable living

### Core Gameplay

- Waste items move across the screen on a conveyor belt (NPCs with lateral patrol behaviour).
- The player controls a sorting mechanism to direct each item into one of four bins: **Recycle**, **Compost**, **Landfill**, or **E-Waste**.
- **Correct sorts** earn points and display an educational fact about the item (e.g., "Aluminium cans can be recycled indefinitely").
- **Incorrect sorts** deduct a life and show the correct bin with an explanation.
- The game is organised into waves of increasing speed and complexity.
- **Bonus rounds** introduce "tricky items" — things commonly mis-sorted (e.g., a greasy pizza box goes to Compost, not Recycle).
- A running accuracy percentage encourages players to be both fast and correct.

### How It Addresses the Problem

- Repeated sorting of common household items builds correct sorting habits through muscle memory.
- Immediate corrective feedback ensures players learn from mistakes.
- Tricky items specifically target common misconceptions.
- Fun facts provide context and reasoning behind sorting rules, promoting deeper understanding.
- Progressive difficulty maintains engagement across skill levels.

### Engine Utilisation

| Engine Subsystem       | Application in EcoSort                                         |
|------------------------|----------------------------------------------------------------|
| EntityManager          | Manages waste items, bins, conveyor belt elements              |
| MovementManager        | Controls conveyor belt movement and item motion                |
| CollisionManager       | Detects item–bin contact for sorting validation                |
| SceneManager           | Transitions between Menu, Tutorial, Gameplay, Bonus, Results   |
| TimeManager            | Wave timing, speed progression, bonus round countdowns         |
| InputOutputManager     | Player sorting controls, sound effects for correct/wrong sorts |

### Design Patterns

1. **Factory Pattern** — A `WasteItemFactory` generates waste items from a data-driven configuration. Each item is defined by its name, correct bin category, point value, speed, and difficulty rating. Adding a new waste item requires only a new data entry — no code modifications.

2. **Strategy Pattern** — Conveyor movement behaviours are implemented as interchangeable strategies: `LinearStrategy` (straight across), `ZigZagStrategy` (erratic movement), and `SpeedUpStrategy` (accelerating items). Higher difficulty waves use more challenging strategies.

3. **State Pattern** — Game entities transition through clearly defined states. Items move through `OnBelt → BeingSorted → Sorted (Correct/Wrong)`. The game itself transitions through `Tutorial → Playing → BonusRound → WaveComplete → GameOver`. This eliminates complex conditional logic and makes behaviour transitions explicit and maintainable.

### Scalability

- Adding a new waste item: Add a new entry to the item data configuration — no changes to existing classes.
- Adding a new bin category: Create a new bin entity and update the sorting validation — all existing items and logic remain unchanged.
- Adding a new wave/difficulty: Configure a new wave with speed, item pool, and strategy parameters.
- Adding a new conveyor behaviour: Implement the `MovementStrategy` interface — existing behaviours are unaffected.

---

## Proposal 3: TrailBlazer — Environmental Cleanup Adventure

### Problem Statement

Parks, trails, and natural areas worldwide are suffering from increasing levels of litter and pollution. At the same time, younger generations are growing increasingly disconnected from nature — spending less time outdoors and having limited awareness of local ecosystems and the environmental impact of waste. This combination leads to a cycle where people who feel disconnected from nature are less motivated to protect it, resulting in further environmental degradation.

### Proposed Solution

TrailBlazer is an interactive environmental education game in which the player takes on the role of a park ranger tasked with cleaning up polluted trails across different natural biomes. Players explore trail environments, collecting scattered litter while carefully navigating around wildlife that must not be disturbed. As the trail is cleaned, the environment visually transforms — revealing its natural beauty and attracting wildlife back to the area. The game teaches environmental awareness, waste identification, and respect for wildlife through direct, rewarding gameplay.

### Target Audience

- Primary: Children and young teenagers aged 10–16
- Secondary: Families, schools, and environmental education programmes

### Core Gameplay

#### Movement and Exploration

- The player controls a park ranger character that moves freely through a scrolling 2D trail environment using WASD or arrow keys.
- Each level represents a different biome: **Forest Trail**, **Beach Shoreline**, **Mountain Path**, and **River Bank**.
- The trail is littered with various waste items scattered across the environment, each requiring the player to walk over them to collect.

#### Litter Collection

- Litter items are entities with different movement behaviours:
  - **Static litter** (glass bottles, food wrappers) remains in place and is straightforward to collect.
  - **Wind-blown litter** (plastic bags, paper cups, crisp packets) drifts across the screen with wind-based movement, requiring the player to chase and intercept them.
  - **Waterborne litter** (bottles in rivers, debris on shorelines) follows current-based movement patterns unique to water biomes.
- Each litter item has a point value and a waste category (plastic, paper, glass, organic, hazardous).
- Collecting an item displays a brief educational fact (e.g., "A plastic bag takes up to 1,000 years to decompose").

#### Wildlife Hazards

- Wildlife NPCs patrol the trail with distinct movement patterns:
  - **Bees** move in circular patrol patterns around nests. Disturbing them (colliding) causes the player to retreat and lose time.
  - **Snakes** patrol along the ground in linear paths. Contact causes a temporary speed reduction (simulating caution).
  - **Birds** perch in areas and flee if the player approaches too quickly, reducing the biodiversity score.
- The key mechanic is **avoidance and respect** — unlike traditional games, the player must navigate around wildlife rather than confronting it. This reinforces the message of coexisting with nature.

#### Environmental Transformation

- Each biome begins in a visibly polluted state (muted colours, no wildlife, visible litter overlay).
- As the player collects litter, the environment progressively transforms:
  - **0–30% cleaned:** Polluted state. Muted background, no ambient wildlife.
  - **30–60% cleaned:** Recovering state. Background colours brighten, small animals begin to appear at the edges.
  - **60–90% cleaned:** Healthy state. Vibrant colours, butterflies and birds populate the background.
  - **90–100% cleaned:** Pristine state. Full natural beauty revealed, ambient wildlife throughout, bonus visual effects (sunlight, water sparkle).
- This transformation serves as the primary reward mechanism and provides a powerful visual for the demo presentation.

#### Biodiversity Counter

- A real-time biodiversity score is displayed on the HUD, tracking the number and variety of wildlife species that have returned to the area.
- The counter increases as the environment improves and decreases if the player disturbs wildlife through careless movement.
- At the end of each level, a summary screen shows the species restored, litter collected by category, and environmental impact statistics.

#### Progression and Difficulty

- **Forest Trail** (Level 1): Introductory biome with mostly static litter and slow wildlife. Teaches core mechanics.
- **Beach Shoreline** (Level 2): Introduces wind-blown litter and tidal movement patterns. Crabs and seabirds as wildlife.
- **Mountain Path** (Level 3): Narrow paths with more wildlife density. Wind is stronger, blowing litter faster. Mountain goats and eagles.
- **River Bank** (Level 4): Introduces waterborne litter with current-based movement. Fish, frogs, and herons. Requires precise timing to intercept floating debris.
- Each biome has a target cleanup percentage to advance and a par time for bonus scoring.

### How It Addresses the Problem

- **Builds environmental awareness:** Players directly see the impact of litter on natural environments and the positive transformation when it is removed.
- **Teaches waste identification:** Each litter item is categorised, and educational facts reinforce knowledge of decomposition times and environmental harm.
- **Promotes respect for wildlife:** The avoidance mechanic teaches coexistence rather than domination — players learn that wildlife should be observed, not disturbed.
- **Connects youth to nature:** By experiencing different biomes and witnessing their restoration, players develop a sense of stewardship and connection to the natural world.
- **Provides positive reinforcement:** The visual transformation and biodiversity counter reward cleanup efforts, reinforcing the message that individual actions have measurable impact.

### Engine Utilisation

| Engine Subsystem       | Application in TrailBlazer                                                   |
|------------------------|-----------------------------------------------------------------------------|
| EntityManager          | Manages litter items, wildlife NPCs, player, and environmental objects      |
| MovementManager        | Handles player movement, wind-blown litter drift, wildlife patrol paths     |
| CollisionManager       | Detects litter collection (player–litter) and wildlife disturbance (player–animal) |
| SceneManager           | Transitions between Menu, Biome Select, Gameplay, Level Summary, and End    |
| TimeManager            | Level timer, par time tracking, wildlife respawn cooldowns                  |
| InputOutputManager     | Player controls (WASD/Arrow keys), collection sound effects, alert sounds   |

### Design Patterns

1. **Factory Pattern** — A `LitterFactory` class centralises the creation of all litter entities. Each item is configured with properties including waste category, point value, movement type, decomposition fact, and biome availability. Adding a new litter type (e.g., a face mask, a battery) requires only a new factory entry — no modifications to existing game or engine code. Similarly, a `WildlifeFactory` creates animal NPCs with species-specific patrol behaviour, speed, and disturbance response. Adding a new animal species to any biome requires only a new factory configuration.

2. **Strategy Pattern** — Litter and wildlife movement behaviours are implemented as interchangeable strategies behind a `MovementStrategy` interface:
   - `StaticStrategy` — No movement. Used for heavy litter (glass, cans).
   - `WindDriftStrategy` — Lateral drift with randomised gusts. Used for lightweight litter (bags, paper).
   - `CurrentFlowStrategy` — Directional flow following water currents. Used for waterborne debris.
   - `CircularPatrolStrategy` — Orbiting movement around a fixed point. Used for bees.
   - `LinearPatrolStrategy` — Back-and-forth movement along a path. Used for snakes and ground animals.
   - `FleeStrategy` — Moves away from the player when proximity threshold is breached. Used for birds.

   New movement behaviours can be added by implementing the `MovementStrategy` interface without modifying any existing entity or strategy class.

3. **Observer Pattern** — Multiple independent systems observe game events without coupling to one another:
   - `BiodiversityTracker` observes litter collection events and wildlife disturbance events to maintain the biodiversity score. It independently determines when new species should appear based on cleanup progress.
   - `EnvironmentRenderer` observes the cleanup percentage to trigger visual transformation stages (polluted → recovering → healthy → pristine).
   - `StatisticsCollector` observes all collection and disturbance events to compile the end-of-level summary.

   Each observer operates independently — removing or adding an observer has no impact on the game logic or other observers.

4. **State Pattern** — The environment transitions through clearly defined states that govern visual presentation and gameplay behaviour:
   - `PollutedState` — Muted visuals, no ambient wildlife, maximum litter density.
   - `RecoveringState` — Brightening visuals, occasional wildlife sightings, reduced litter.
   - `HealthyState` — Vibrant visuals, active wildlife, scattered remaining litter.
   - `PristineState` — Full visual beauty, abundant wildlife, bonus effects.

   Each state encapsulates its own rendering logic and wildlife spawn rules, eliminating complex conditional checks in the main game loop. State transitions are triggered automatically based on cleanup progress.

### Scalability

- **Adding a new litter type:** Create a new entry in `LitterFactory` with its properties and assign a `MovementStrategy`. No changes to existing classes.
- **Adding a new biome/level:** Create a new `Scene` subclass with its own litter spawn configuration, wildlife roster, and environmental colour palette. Register it in the `SceneManager`. Existing biomes are unaffected.
- **Adding a new wildlife species:** Create a new entry in `WildlifeFactory` with species-specific movement strategy and disturbance response. No changes to existing wildlife or game logic.
- **Adding a new movement behaviour:** Implement the `MovementStrategy` interface. Existing strategies and entities remain unchanged.
- **Adding a new environment state:** Implement the `EnvironmentState` interface with its rendering logic and spawn rules. Existing states are unaffected.
- **Adding a new observer/tracker:** Implement the observer interface and register it. No changes to the event source or other observers.

### Presentation and Demo Potential

TrailBlazer offers a particularly strong visual narrative for the 10-minute presentation video:
- The environmental transformation from polluted to pristine is immediately visible and impactful.
- Side-by-side comparison of a polluted biome vs. a cleaned biome demonstrates the game's educational message in seconds.
- Live addition of a new litter type or wildlife species during the demo proves scalability concretely.
- The biodiversity counter provides a quantitative metric that makes the player's impact tangible.

---

## Summary

All three proposals are designed to:

- Address a real-world educational problem with a clearly defined target audience.
- Fully utilise the Abstract Engine's subsystems, demonstrating the engine's generality and reusability.
- Implement at least three design patterns in a manner that is organic to the game's requirements.
- Maintain a clear separation between engine code (abstract, reusable) and game code (concrete, problem-specific).
- Prioritise scalability — new content can be added with minimal or no changes to existing code.

We welcome feedback on any proposal and are happy to proceed with whichever best aligns with the module's expectations.
