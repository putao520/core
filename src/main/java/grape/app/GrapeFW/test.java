package main.java.grape.app.GrapeFW;

import common.java.database.DbLayerHelper;

public class test extends DbLayerHelper {

    public test() {
        //super("mysql","configs","id");
        super("mongodb", "test");
    }

}
