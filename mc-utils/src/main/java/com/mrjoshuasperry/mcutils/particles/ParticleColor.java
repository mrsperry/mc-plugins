package com.mrjoshuasperry.mcutils.particles;

public class ParticleColor {
    public double red;
    public double green;
    public double blue;

    public static ParticleColor fromHex(String hex) {
        ParticleColor color = new ParticleColor();

        try {
            double tempRed = Integer.parseInt(hex.substring(0, 2), 16);
            color.red = tempRed <= 20 ? -1 : tempRed / 255.0;
            color.green = Integer.parseInt(hex.substring(2, 4), 16) / 255.0;
            color.blue = Integer.parseInt(hex.substring(4, 6), 16) / 255.0;

            return color;
        } catch (Exception e) {
            return null;
        }
    }

    public static ParticleColor fromDecimal(int r, int g, int b) {
        ParticleColor color = new ParticleColor();

        color.red = (r <= 20) ? -1 : r / 255.0;
        color.green = g / 255.0;
        color.blue = b / 255.0;

        return color;
    }
}
