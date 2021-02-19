package main.java.grape.app.GrapeFW;

import common.java.Database.DbLayerHelper;

public class test extends DbLayerHelper {

    public test() {
        //super("mysql","configs","id");
        super("mongodb", "test");
    }

}
