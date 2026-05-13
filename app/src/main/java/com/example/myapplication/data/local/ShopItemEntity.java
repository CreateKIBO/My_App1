package com.example.myapplication.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shop_items")
public class ShopItemEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String type;
    private String name;
    private String description;
    private int price;
    private String iconResName;
    private String colorHex;
    private String emoji;
    private String colorHexDark;
    @NonNull
    @ColumnInfo(name = "source", defaultValue = "shop")
    private String source = "shop"; // "shop" or "streak" — streak items cannot be purchased

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public String getIconResName() { return iconResName; }
    public void setIconResName(String iconResName) { this.iconResName = iconResName; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public String getColorHexDark() { return colorHexDark; }
    public void setColorHexDark(String colorHexDark) { this.colorHexDark = colorHexDark; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    @NonNull
    public static ShopItemEntity[] getDefaultItems() {
        return new ShopItemEntity[]{
                // Avatars
                create("AVATAR", "勇士", "无畏前行的冒险者", 0, "ic_avatar_warrior", "#F59E0B", "#B45309", "⚔️", "shop"),
                create("AVATAR", "魔法师", "掌控元素的智者", 0, "ic_avatar_mage", "#7C3AED", "#5B21B6", "🧙", "shop"),
                create("AVATAR", "忍者", "暗影中的执行者", 0, "ic_avatar_ninja", "#16A34A", "#15803D", "🥷", "shop"),
                create("AVATAR", "骑士", "守护正义的盾牌", 0, "ic_avatar_knight", "#EA580C", "#C2410C", "🛡️", "shop"),
                create("AVATAR", "龙人", "龙族血脉的继承者", 300, "ic_avatar_dragon", "#DC2626", "#B91C1C", "🐉", "shop"),
                create("AVATAR", "机器人", "来自未来的机械体", 250, "ic_avatar_robot", "#3B82F6", "#2563EB", "🤖", "shop"),
                create("AVATAR", "猫咪", "慵懒而敏捷的伙伴", 200, "ic_avatar_cat", "#F59E0B", "#D97706", "🐱", "shop"),
                create("AVATAR", "幽灵", "飘渺不定的灵魂", 350, "ic_avatar_ghost", "#6366F1", "#4F46E5", "👻", "shop"),
                // Streak-exclusive avatars (price=-1 means not purchasable)
                create("AVATAR", "凤凰", "浴火重生的连续传说", -1, "ic_avatar_phoenix", "#EF4444", "#B91C1C", "🔥", "streak"),
                create("AVATAR", "星辰", "永恒闪耀的自律之星", -1, "ic_avatar_star", "#F59E0B", "#B45309", "⭐", "streak"),
                // Themes
                create("THEME", "默认蓝", "清爽经典的蓝色调", 0, "ic_theme_default", "#3B82F6", "#2563EB", "🎨", "shop"),
                create("THEME", "森林绿", "自然宁静的绿色", 400, "ic_theme_forest", "#16A34A", "#15803D", "🌲", "shop"),
                create("THEME", "日落橙", "温暖活力的橙色调", 400, "ic_theme_sunset", "#EA580C", "#C2410C", "🌅", "shop"),
                create("THEME", "樱花粉", "柔美浪漫的粉色", 450, "ic_theme_sakura", "#DB2777", "#BE185D", "🌸", "shop"),
                create("THEME", "暗夜紫", "神秘深邃的紫色", 500, "ic_theme_night", "#7C3AED", "#5B21B6", "🌙", "shop"),
                create("THEME", "海洋蓝", "深沉广阔的蓝色", 400, "ic_theme_ocean", "#0369A1", "#075985", "🌊", "shop"),
                create("THEME", "沙漠金", "金色沙漠的温暖", 450, "ic_theme_desert", "#B45309", "#92400E", "🏜️", "shop"),
                create("THEME", "极光绿", "北极光的奇幻色彩", 500, "ic_theme_aurora", "#0D9488", "#0F766E", "🌌", "shop"),
                // Streak-exclusive themes
                create("THEME", "烈焰红", "连续打卡的炽热证明", -1, "ic_theme_flame", "#DC2626", "#991B1B", "🔥", "streak"),
                create("THEME", "星河紫", "60天连续的至高荣耀", -1, "ic_theme_galaxy", "#6D28D9", "#4C1D95", "✨", "streak"),
        };
    }

    private static ShopItemEntity create(String type, String name, String desc, int price, String icon, String color, String colorDark, String emoji, String source) {
        ShopItemEntity item = new ShopItemEntity();
        item.setType(type);
        item.setName(name);
        item.setDescription(desc);
        item.setPrice(price);
        item.setIconResName(icon);
        item.setColorHex(color);
        item.setColorHexDark(colorDark);
        item.setEmoji(emoji);
        item.setSource(source);
        return item;
    }
}
