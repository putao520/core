package common.java.database;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DbFilter {
	private JSONArray condArray;
	private List<List<Object>> conds;
	private boolean conditiobLogicAnd;

	private DbFilter() {
		reinit();
	}

	private DbFilter(JSONArray cond) {
		condArray = cond == null ? new JSONArray() : cond;
	}

	public static final DbFilter buildDbFilter() {
		return new DbFilter();
	}

	public static final DbFilter buildDbFilter(JSONArray cond) {
		return new DbFilter(cond);
	}

	private void reinit() {
		conditiobLogicAnd = true;
		condArray = new JSONArray();
		conds = new ArrayList<>();
	}

	public DbFilter and() {
		conditiobLogicAnd = true;
		return this;
	}

	public DbFilter or() {
		conditiobLogicAnd = false;
		return this;
	}

	public DbFilter eq(String field, Object value) {//One Condition

		addCondition(field, value, "=");
		return this;
	}

	public DbFilter ne(String field, Object value) {//One Condition

		addCondition(field, value, "!=");
		return this;
	}

	public DbFilter gt(String field, Object value) {//One Condition

		addCondition(field, value, ">");
		return this;
	}

	public DbFilter lt(String field, Object value) {//One Condition

		addCondition(field, value, "<");
		return this;
	}

	public DbFilter gte(String field, Object value) {//One Condition

		addCondition(field, value, ">=");
		return this;
	}

	public DbFilter lte(String field, Object value) {//One Condition

		addCondition(field, value, "<=");
		return this;
	}

	public DbFilter like(String field, Object value) {

		addCondition(field, value, "like");
		return this;
	}

	@SuppressWarnings("unchecked")
	private void addCondition(String field, Object value, String logic) {
		condArray.add((new JSONObject("field", field)).puts("logic", logic).puts("value", value));
	}

	public JSONArray build() {
		return condArray;
	}

	public DbFilter groupCondition(List<List<Object>> conds) {
		//List<List<Object>> nowConds = this.conds;
		List<Object> block = new ArrayList<>();
		block.add(conditiobLogicAnd ? "and" : "or");
		block.add(conds);
		this.conds.add(block);
		return this;
	}

	public boolean nullCondition() {
		return condArray.size() == 0;
	}

	public List<List<Object>> buildex() {
		List<Object> bit;
		if (condArray.size() > 0) {
			for (Object obj : condArray) {
				bit = new ArrayList<>();
				JSONObject json = (JSONObject) obj;
				bit.add(conditiobLogicAnd ? "and" : "or");
				bit.add(json.getString("field"));
				bit.add(json.getString("logic"));
				bit.add(json.get("value"));
				conds.add(bit);
			}
		}
		List<List<Object>> r = conds;
		reinit();
		return r;
	}
}
