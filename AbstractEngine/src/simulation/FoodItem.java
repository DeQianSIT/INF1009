package simulation;

import engine.entities.NPC;

/**
 * Food item entity - can be good (healthy) or bad (unhealthy) food.
 * Used for the nutrition-based collection game.
 */
public class FoodItem extends NPC {
    private final String foodName;
    private final boolean goodFood;

    public FoodItem(float x, float y, String behavior, String foodName, boolean goodFood) {
        super(x, y, behavior);
        this.foodName = foodName;
        this.goodFood = goodFood;
    }

    public String getFoodName() {
        return foodName;
    }

    public boolean isGoodFood() {
        return goodFood;
    }
}
