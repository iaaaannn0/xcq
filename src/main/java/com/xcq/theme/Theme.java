package com.xcq.theme;

import java.awt.*;

public class Theme {
    private String name = "default";
    private Color backgroundColor = new Color(245, 245, 245);
    private Color inputBackgroundColor = Color.WHITE;
    private Color textColor = Color.BLACK;
    private Color buttonColor = new Color(230, 230, 230);
    private Font font = new Font("微软雅黑", Font.PLAIN, 12);

    public Theme() {
    }

    public Theme(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getInputBackgroundColor() {
        return inputBackgroundColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    public Color getButtonColor() {
        return buttonColor;
    }

    public Font getFont() {
        return font;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setInputBackgroundColor(Color inputBackgroundColor) {
        this.inputBackgroundColor = inputBackgroundColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    public void setButtonColor(Color buttonColor) {
        this.buttonColor = buttonColor;
    }

    public void setFont(Font font) {
        this.font = font;
    }
} 