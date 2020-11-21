package main.interfaceApplication;

import common.java.JGrapeSystem.GscBooter;
import org.json.simple.JSONObject;

public class test {
    public static void main(String[] args) {

        System.out.println("-");
        System.out.println("GrapeFW!");
        String[] packs = test.class.getPackage().getName().split("\\.");
        //System.setProperty("-Dco.paralleluniverse.fibers.detectRunawayFibers", String.valueOf(false));
        System.out.println("启动服务...");
        System.setProperty("AppName", packs[packs.length - 1]);
        try {
            GscBooter.start("测试服务");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("服务器崩溃!");
        }
        System.out.println("-");
        /*
         * localdb
         * localcache
         * 2个配置必须存在而且有效
         * */
    }

    public JSONObject test(String a, int b) {
        return (new JSONObject()).puts("测试信息", a).puts("测试参数", b);
    }
}
