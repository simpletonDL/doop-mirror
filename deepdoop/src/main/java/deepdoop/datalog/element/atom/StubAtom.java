package deepdoop.datalog.element.atom;

import deepdoop.actions.IVisitable;
import deepdoop.actions.IVisitor;
import deepdoop.datalog.expr.VariableExpr;
import java.lang.UnsupportedOperationException;
import java.util.List;

// Special class for when only a string is actually present but we need to
// treat it as an atom object
public class StubAtom implements IAtom {

	public final String name;

	public StubAtom(String name) {
		this.name = name;
	}

	@Override
	public IVisitable accept(IVisitor v) {
		v.enter(this);
		return v.exit(this, null);
	}

	@Override
	public String name() { return name; }
	@Override
	public String stage() { return null; }
	@Override
	public int arity() { throw new UnsupportedOperationException(); }
	@Override
	public List<VariableExpr> getVars() { throw new UnsupportedOperationException(); }
	@Override
	public IAtom instantiate(String stage, List<VariableExpr> vars) { throw new UnsupportedOperationException(); }

	@Override
	public String toString() {
		//return name;
		return "STUB: " + name;
	}
}
