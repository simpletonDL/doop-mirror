package deepdoop.datalog;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Functional implements IAtom {

	String      _name;
	int         _arity;
	List<IExpr> _keyExprs;
	IExpr       _valueExpr;
	// Declaration
	List<IAtom> _keyTypes;
	IAtom       _valueType;
	// Instance
	String      _stage;
	boolean     _inDecl;


	public Functional(String name, List<IExpr> keyExprs, IExpr valueExpr, List<IAtom> keyTypes, IAtom valueType) {
		assert (keyTypes == null && valueType == null) || keyExprs.size() == keyTypes.size();
		_name      = name;
		_arity     = keyExprs.size() + 1;
		_keyExprs  = keyExprs;
		_valueExpr = valueExpr;
		_keyTypes  = keyTypes;
		_valueType = valueType;
		_inDecl    = true;
	}
	public Functional(String name, List<IExpr> keyExprs, IExpr valueExpr) {
		this(name, keyExprs, valueExpr, null, null);
	}
	public Functional(String name, String stage, List<IExpr> keyExprs, IExpr valueExpr) {
		_name      = name;
		_arity     = keyExprs.size() + 1;
		_keyExprs  = keyExprs;
		_valueExpr = valueExpr;
		_stage     = stage;
		_inDecl    = false;
	}


	public void setTypes(List<IAtom> keyTypes, IAtom valueType) {
		assert _keyTypes == null && _valueType == null;
		assert _arity == keyTypes.size() + 1;
		_keyTypes  = keyTypes;
		_valueType = valueType;
	}

	@Override
	public String name() {
		return _name;
	}

	@Override
	public IAtom.Type type() {
		return IAtom.Type.FUNCTIONAL;
	}

	@Override
	public int arity() {
		return _arity;
	}

	@Override
	public List<VariableExpr> getVars() {
		List<VariableExpr> list = new ArrayList<>();
		for (IExpr e : _keyExprs)
			list.add((e instanceof VariableExpr ? (VariableExpr) e : null));
		list.add((_valueExpr instanceof VariableExpr ? (VariableExpr) _valueExpr : null));
		return list;
	}

	@Override
	public Functional init(String id) {
		if (_inDecl) {
			List<IAtom> newKeyTypes = new ArrayList<>();
			for (IAtom t : _keyTypes) newKeyTypes.add(t.init(id));
			return new Functional(Names.nameId(_name, id), _keyExprs, _valueExpr, newKeyTypes, _valueType.init(id));
		}
		else {
			List<IExpr> newKeyExprs = new ArrayList<>();
			for (IExpr e : _keyExprs) newKeyExprs.add(e.init(id));
			return new Functional(Names.nameId(_name, id), _stage, newKeyExprs, _valueExpr.init(id));
		}
	}

	@Override
	public String toString() {
		if (_inDecl) {
			StringJoiner joiner1 = new StringJoiner(", ");
			StringJoiner joiner2 = new StringJoiner(", ");
			for (int i = 0 ; i < _arity - 1 ; i++) {
				IExpr v = _keyExprs.get(i);
				IAtom t = _keyTypes.get(i);
				joiner1.add(v.toString());
				joiner2.add(t.name() + "(" + v + ")");
			}
			joiner2.add(_valueType.name() + "(" + _valueExpr + ")");
			return _name + "[" + joiner1 + "] = " + _valueExpr + " -> " + joiner2 + ".";
		}
		else {
			StringJoiner joiner = new StringJoiner(", ");
			for (IExpr e : _keyExprs) joiner.add(e.toString());
			return Names.nameStage(_name, _stage) + "[" + joiner + "] = " + _valueExpr;
		}
	}
}
