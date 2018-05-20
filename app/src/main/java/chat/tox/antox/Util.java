package chat.tox.antox;

public class Util
{

    public static int clamp(int value, int min, int max)
    {
        return Math.min(Math.max(value, min), max);
    }
}