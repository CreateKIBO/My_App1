package com.example.myapplication.util;

public class RewardCalculator {

    public static final String CAT_WORK = "Work";
    public static final String CAT_STUDY = "Study";
    public static final String CAT_EXERCISE = "Exercise";
    public static final String CAT_PERSONAL = "Personal";
    public static final String CAT_OTHER = "Other";

    public static final String TYPE_AVATAR = "AVATAR";
    public static final String TYPE_THEME = "THEME";
    public static final String TYPE_PROP = "PROP";
    public static final String TX_COIN = "COIN";
    public static final String TX_XP = "XP";
    public static final String TX_SPEND = "SPEND";
    public static final String TX_LEVEL_UP = "LEVEL_UP";

    private static final int BASE_COINS = 10;
    private static final int BASE_XP = 25;

    private static final int WORK_BONUS_COINS = 5;
    private static final int WORK_BONUS_XP = 10;
    private static final int STUDY_BONUS_COINS = 5;
    private static final int STUDY_BONUS_XP = 15;
    private static final int EXERCISE_BONUS_COINS = 8;
    private static final int EXERCISE_BONUS_XP = 10;

    private static final int ALL_COMPLETE_BONUS_COINS = 30;
    private static final int ALL_COMPLETE_BONUS_XP = 50;

    public static final int POMODORO_XP = 15;
    public static final int POMODORO_COINS = 5;
    public static final int REVIEW_XP = 5;
    public static final int REVIEW_COINS = 2;
    public static final int[] REVIEW_INTERVALS = {1, 2, 4, 7, 15, 30};

    private static final int XP_PER_LEVEL = 200;

    // Level-up bonus: level * 50 coins
    private static final int LEVEL_UP_COIN_MULTIPLIER = 50;

    public static class Reward {
        public final int coins;
        public final int xp;

        public Reward(int coins, int xp) {
            this.coins = coins;
            this.xp = xp;
        }
    }

    public static Reward calculateTaskReward(String category) {
        int coins = BASE_COINS;
        int xp = BASE_XP;

        switch (category) {
            case CAT_WORK:
                coins += WORK_BONUS_COINS;
                xp += WORK_BONUS_XP;
                break;
            case CAT_STUDY:
                coins += STUDY_BONUS_COINS;
                xp += STUDY_BONUS_XP;
                break;
            case CAT_EXERCISE:
                coins += EXERCISE_BONUS_COINS;
                xp += EXERCISE_BONUS_XP;
                break;
        }

        return new Reward(coins, xp);
    }

    public static Reward getDayCompleteBonus() {
        return new Reward(ALL_COMPLETE_BONUS_COINS, ALL_COMPLETE_BONUS_XP);
    }

    public static Reward calculateStreakBonus(int streakDays) {
        if (streakDays >= 30) return new Reward(100, 200);
        if (streakDays >= 14) return new Reward(50, 100);
        if (streakDays >= 7) return new Reward(30, 50);
        if (streakDays >= 3) return new Reward(15, 20);
        return new Reward(0, 0);
    }

    public static int calculateLevel(int totalXp) {
        return (totalXp / XP_PER_LEVEL) + 1;
    }

    public static int getXpForNextLevel(int currentLevel) {
        return currentLevel * XP_PER_LEVEL;
    }

    public static int getXpProgressInLevel(int totalXp) {
        return totalXp % XP_PER_LEVEL;
    }

    public static int getLevelUpBonusCoins(int newLevel) {
        return newLevel * LEVEL_UP_COIN_MULTIPLIER;
    }

    public static String getLevelTitle(int level) {
        if (level >= 51) return "至尊王者";
        if (level >= 41) return "传奇大师";
        if (level >= 31) return "无双勇者";
        if (level >= 21) return "精通行者";
        if (level >= 11) return "专注先锋";
        if (level >= 6) return "进阶冒险者";
        return "新手冒险者";
    }
}
