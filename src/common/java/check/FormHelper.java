/*
 * 字段检查对象
 * */
package common.java.check;

import common.java.apps.MModelRuleNode;
import common.java.number.NumberHelper;
import common.java.session.Session;
import common.java.session.UserSession;
import common.java.string.StringHelper;
import common.java.time.TimeHelper;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FormHelper {
    private final HashMap<String, MModelRuleNode> checkObject;
    private final List<String> maskCache;
    private boolean checkState;
    private String lastErrorKey;

    public FormHelper() {
        lastErrorKey = null;
        checkState = true;
        checkObject = new HashMap<>();
        maskCache = new ArrayList<>();
    }

    //返回false不检查，直接算过
    private boolean checkAuth() {
        boolean oldState;
        if (!checkState) {
            checkState = true;
            oldState = true;
        } else {
            checkState = false;
            oldState = false;
        }
        return oldState;
    }

    public FormHelper stopCheck() {
        checkState = false;
        return this;
    }

    public FormHelper resumeCheck() {
        checkState = true;
        return this;
    }

    //添加字段对象
    public FormHelper addField(MModelRuleNode field) {
        checkObject.put(field.field(), field);
        maskCache.clear();
        return this;
    }

    //过滤保护字段，从输入字段中删除保护属性的字段
    public JSONObject filterProtect(JSONObject inputJson) {
        if (checkAuth() && inputJson != null && inputJson.size() > 0) {
            MModelRuleNode tf;
            JSONObject rjson = new JSONObject();
            for (String key : inputJson.keySet()) {
                tf = checkObject.get(key);
                if (tf == null || tf.type() != MModelRuleNode.FieldType.protectField) {
                    rjson.put(key, inputJson.get(key));
                }
            }
            inputJson = rjson;
        }
        return inputJson;
    }

    public String filterMask(String inputField) {
        return filterMask(inputField.split(","));
    }

    public String filterMask(String[] fields) {
        String rs;
        if (checkAuth()) {
            MModelRuleNode tf;
            List<String> newfield = new ArrayList<>();
            int l = fields.length;
            for (String field : fields) {
                if (checkObject.containsKey(field)) {
                    tf = checkObject.get(field);
                    if (tf.type() != MModelRuleNode.FieldType.maskField) {
                        newfield.add(field);
                    }
                }
            }
            rs = StringHelper.join(newfield);
        } else {
            rs = StringHelper.join(fields);
        }
        return rs;
    }

    //删除字段对象
    public FormHelper removeField(String fieldName) {
        if (checkObject.containsKey(fieldName)) {
            checkObject.remove(fieldName);
            maskCache.clear();
        }
        return this;
    }

    public FormHelper importField(HashMap<String, MModelRuleNode> newObj) {
        checkObject.putAll(newObj);
        maskCache.clear();
        return this;
    }

    //检查各个字段是否符合要求,也可以同时开启严格模式，要求字段必须符合要求
    public boolean checkTable(JSONObject inputData, boolean strict) {
        return _checkTable(inputData, false, strict);
    }

    //自己补齐字段
    public String autoComplete(String inputData, HashMap<String, MModelRuleNode> permInfos) {
        return autoComplete(JSONObject.toJSON(inputData), permInfos).toJSONString();
    }

    /**
     * 根据gsc-model( 包含 db-model和 perm-model)自动补充入库前的数据
     */
    public JSONObject autoComplete(JSONObject inputData, HashMap<String, MModelRuleNode> permInfos) {
        MModelRuleNode tf;
        HashMap<String, MModelRuleNode> waitCheck = new HashMap<>();
        // 填充db-model定义
        waitCheck.putAll(checkObject);
        // 填充符合当前会话的权限到db-model的定义
        waitCheck.putAll(permInfos);
        for (String key : waitCheck.keySet()) {
            if (!inputData.containsKey(key)) {
                tf = waitCheck.get(key);
                inputData.put(key, autoValueFilter(tf.initValue()));
            }
        }
        return inputData;
    }

    /**
     * 判断当前值是否是特殊定义的值,如果是自动填充值
     * :timestamp	=>	当前时间戳
     */
    private Object autoValueFilter(Object nowValue) {
        Object rs = nowValue;
        if (nowValue instanceof String) {
            switch (nowValue.toString()) {
                case ":timestamp" -> rs = TimeHelper.build().nowMillis();
                case ":user" -> {
                    Session se = UserSession.current();
                    if (se.checkSession()) {
                        rs = UserSession.current().getUID();
                    }
                }
                case ":group" -> {
                    Session se = UserSession.current();
                    if (se.checkSession()) {
                        rs = UserSession.current().getGID();
                    }
                }
                default -> {
                }
            }
        }
        return rs;
    }

    /**
     * @param inputData 输入的数据
     * @param filter    是否开启过滤模式
     */
    private boolean _checkTable(JSONObject inputData, boolean filter, boolean strict) {
        MModelRuleNode tField;
        boolean rs;
        lastErrorKey = null;
        if (checkAuth()) {
            for (String key : checkObject.keySet()) {
                tField = checkObject.get(key);
                if (inputData.containsKey(key)) {
                    CheckType checkType = tField.checkType();
                    Object val = inputData.get(key);
                    rs = checkType.forEachOr(in -> {
                        for (int j : in) {
                            if (!this.check(j, val)) {
                                return false;
                            }
                        }
                        return true;
                    });
                    if (!rs) {
                        if (filter) {
                            inputData.put(key, tField.failedValue());
                        }
                    }

                } else {
                    if (filter) {
                        inputData.put(key, tField.initValue());
                    }
                    rs = false;
                }
                if (filter) {
                    continue;
                }
                if (strict) {
                    if (!rs) {
                        lastErrorKey = key;
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public String getlastErrorName() {
        return lastErrorKey;
    }

    //检查数据，不符合的使用默认值填充
    public boolean filterAndCheckTable(JSONObject inputData, boolean strict) {
        return _checkTable(inputData, strict, true);
    }

    //返回字段信息
    public String[] getMaskFields() {
        if (maskCache.isEmpty()) {
            MModelRuleNode tf;
            for (String key : checkObject.keySet()) {
                tf = checkObject.get(key);
                if (tf != null) {
                    if (tf.type() == MModelRuleNode.FieldType.maskField) {
                        maskCache.add(key);
                    }
                }
            }
        }
        String[] nStringArray = null;
        if (maskCache.size() > 0) {
            nStringArray = new String[maskCache.size()];
            maskCache.toArray(nStringArray);
        }
        return nStringArray;
    }

    /**
     * @param ruleID 规则混合值
     * @return 返回去掉长度效验后的真实效验值, 失败则返回-1
     */
    private int checkLength(int ruleID, Object val) {
        if (ruleID < formdef.mixSpilt) {
            return ruleID;
        }
        String ruleString = String.valueOf(ruleID);
        int _ruleID = NumberHelper.number2int(StringHelper.build(ruleString).right(2).toString());
        int _maxLen = NumberHelper.number2int(StringHelper.build(ruleString).left(ruleString.length() - 2));
        return val.toString().length() <= _maxLen ? _ruleID : -1;
    }

    /**
     * 实际检查规则和数据
     * 通过为真，不通过为假
     */
    private boolean check(int rule, Object val) {
        String str = StringHelper.any2String(val);
        if (str == null) {
            str = "";
        }
        int ruleID = checkLength(rule, str);
        switch (ruleID) {
            case formdef.notNull:
                return !CheckHelper.isNull(str);
            case formdef.eqNull:
                return CheckHelper.isNull(str);
            case formdef.bigZero:
                return CheckHelper.bigZero(str);
            case formdef.smallZero:
                return !CheckHelper.bigZero(str) && !CheckHelper.eqZero(str);
            case formdef.eqZero:
                return CheckHelper.eqZero(str);
            case formdef.number:
                return CheckHelper.isInt(str);
            case formdef.Int:
                return CheckHelper.isNum(str);
            case formdef.money:
                return CheckHelper.IsDecimal(str);
            case formdef.decimal:
                return CheckHelper.isNum(str) && !CheckHelper.isInt(str);
            case formdef.email:
                return CheckHelper.checkEmail(str);
            case formdef.mobile:
                return CheckHelper.checkMobileNumber(str);
            case formdef.BusniessID:
                return CheckHelper.checkBusinessRegisterNo(str);
            case formdef.Chinese:
                return CheckHelper.checkChinese(str);
            case formdef.id:
                return CheckHelper.checkID(str, 15);
            case formdef.noSpace:
                return CheckHelper.noSpace(str);
            case formdef.name:
                return CheckHelper.checkRealName(str);
            case formdef.PersonID:
                return CheckHelper.checkPersonCardID(str);
            case formdef.simpleDate:
                return CheckHelper.checkDate(str);
            case formdef.week:
                return CheckHelper.checkWeek(str);
            case formdef.month:
                return CheckHelper.checkMonth(str);
            case formdef.ip:
                return CheckHelper.isIP(str);
            case formdef.url:
                return CheckHelper.IsUrl(str);
            case formdef.password:
                return CheckHelper.IsPassword(str);
            case formdef.postCode:
                return CheckHelper.IsPostalcode(str);
            case formdef.Date:
                return CheckHelper.IsDateAndYear(str);
            case formdef.Time:
                return CheckHelper.IsTime(str);
            case formdef.UnixDate:
                try {
                    long l = Long.parseLong(str);
                    return CheckHelper.checkUnixDate(l);
                } catch (Exception e) {
                }
                break;
            default:
                // rule不在有效范围,效验失败
                return false;
        }
        return false;
    }

    public static class formdef {
        /**
         * 复合规则分界线
         * <p>
         * |--|---
         * /效验规则/最大字符长度
         */
        public final static int mixSpilt = 1000;
        /**
         * 不为空
         */
        public final static int notNull = 1;
        /**
         * 为空
         */
        public final static int eqNull = 2;
        /**
         * 大于0
         */
        public final static int bigZero = 3;
        /**
         * 小于0
         */
        public final static int smallZero = 4;
        /**
         * 等于0
         */
        public final static int eqZero = 5;
        /**
         * 整数
         */
        public final static int number = 6;
        /**
         * 自然数，包含小数
         */
        public final static int Int = 7;
        /**
         * 小数点后有2位
         */
        public final static int money = 8;
        /**
         * 小数
         */
        public final static int decimal = 9;
        /**
         * email
         */
        public final static int email = 10;
        /**
         * 手机号
         */
        public final static int mobile = 11;
        /**
         * 工商执照号
         */
        public final static int BusniessID = 12;
        /**
         * 纯中文
         */
        public final static int Chinese = 13;
        /**
         * id
         */
        public final static int id = 26;
        /**
         * 不包含空格
         */
        public final static int noSpace = 14;
        /**
         * 真实姓名
         */
        public final static int name = 15;
        /**
         * 身份证号
         */
        public final static int PersonID = 16;
        /**
         * 常见的有效时间验证，年月日，时分秒
         */
        public final static int simpleDate = 17;
        /**
         * 星期
         */
        public final static int week = 18;
        /**
         * 月
         */
        public final static int month = 19;
        /**
         * ip
         */
        public final static int ip = 20;
        /**
         * url
         */
        public final static int url = 21;
        /**
         * 密码
         */
        public final static int password = 22;
        /**
         * 中国邮政编码
         */
        public final static int postCode = 23;
        /**
         * 支持闰年的年月日，时秒分
         */
        public final static int Date = 24;
        /**
         * 时间
         */
        public final static int Time = 25;
        /**
         * unix时间戳
         */
        public final static int UnixDate = 27;
        /**
         * 银行卡号
         */
        public final static int BankCard = 28;
    }
}
