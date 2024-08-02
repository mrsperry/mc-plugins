package com.mrjoshuasperry.mcutils.sound;

import org.bukkit.Sound;

public class SoundByte {
    public Sound sound;
    public float volume;
    public float pitch;
    public int delay;

    public SoundByte(Sound sound, float volume, float pitch, int delay) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.delay = delay;
    }

    @Override
    public String toString() {
        return "S: " + this.sound.name() + " V: " + this.volume + " P: " + this.pitch + " D: " + this.delay;
    }
}
