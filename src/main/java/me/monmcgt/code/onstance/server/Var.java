package me.monmcgt.code.onstance.server;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.util.Random;

public class Var {
    public static final Gson GSON;
    public static final JsonParser JSON_PARSER;
    public static final Random RANDOM;

    static {
        GSON = new Gson();
        JSON_PARSER = new JsonParser();
        RANDOM = new Random();
    }
}
