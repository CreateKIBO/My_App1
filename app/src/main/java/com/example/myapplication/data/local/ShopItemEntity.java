package com.example.myapplication.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shop_items")
public class ShopItemEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "type")
    private String type;

    @ColumnInfo(name = "price")
    private int price;

    @ColumnInfo(name = "iconResName")
    private String iconResName;

    @ColumnInfo(name = "colorHex")
    private String colorHex;

    public ShopItemEntity() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getIconResName() { return iconResName; }
    public void setIconResName(String iconResName) { this.iconResName = iconResName; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public static ShopItemEntity[] getDefaultItems() {
        return new ShopItemEntity[] {
            // ===== Avatars (pixel art) =====
            item("小勇士", "勇敢的像素冒险者", "AVATAR", 0, "ic_avatar_warrior", "#4CAF50"),
            item("魔法师", "神秘的像素法师", "AVATAR", 50, "ic_avatar_mage", "#7C4DFF"),
            item("忍者", "敏捷的像素忍者", "AVATAR", 80, "ic_avatar_ninja", "#607D8B"),
            item("骑士", "坚毅的像素骑士", "AVATAR", 120, "ic_avatar_knight", "#1565C0"),
            item("龙人", "传说中的像素龙人", "AVATAR", 200, "ic_avatar_dragon", "#FF6F00"),
            item("机器人", "未来感的像素机器人", "AVATAR", 150, "ic_avatar_robot", "#00BCD4"),
            item("猫咪", "可爱的像素猫咪", "AVATAR", 100, "ic_avatar_cat", "#FF9800"),
            item("幽灵", "神秘的像素幽灵", "AVATAR", 180, "ic_avatar_ghost", "#9C27B0"),

            // ===== Themes =====
            item("默认蓝", "经典蓝靛色主题", "THEME", 0, "ic_theme_default", "#3F51B5"),
            item("森林绿", "清新自然主题", "THEME", 100, "ic_theme_forest", "#2E7D32"),
            item("日落橙", "温暖夕阳主题", "THEME", 100, "ic_theme_sunset", "#E65100"),
            item("樱花粉", "浪漫樱花主题", "THEME", 120, "ic_theme_sakura", "#D81B60"),
            item("暗夜紫", "深邃夜空主题", "THEME", 150, "ic_theme_night", "#4A148C"),
            item("海洋蓝", "深海探索主题", "THEME", 120, "ic_theme_ocean", "#01579B"),
            item("沙漠金", "金色沙漠主题", "THEME", 180, "ic_theme_desert", "#F9A825"),
            item("极光绿", "北极极光主题", "THEME", 200, "ic_theme_aurora", "#00BFA5"),
        };
    }

    private static ShopItemEntity item(String name, String desc, String type, int price, String iconRes, String color) {
        ShopItemEntity e = new ShopItemEntity();
        e.setName(name);
        e.setDescription(desc);
        e.setType(type);
        e.setPrice(price);
        e.setIconResName(iconRes);
        e.setColorHex(color);
        return e;
    }
}
