package io.github.zhangt2333.test;

import org.apache.commons.lang3.StringUtils;

public class Main {

    public static String getMsg() {
        return StringUtils.join(new String[]{"Hello", "Maven"}, ", ");
    }

    public static void main(String[] args) {
        System.out.println(getMsg());
    }
}
