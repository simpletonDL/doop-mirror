package org.clyze.deepdoop.datalog.element.atom

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.expr.*

@Canonical
class Entity extends Predicate {

	Entity(String name, String stage=null, IExpr expr) {
		super(name, stage, [expr])
	}

	IAtom newAtom(String stage, List<VariableExpr> vars) {
		assert arity == vars.size()
		assert arity == 1
		new Entity(name, stage, vars.first())
	}
	IAtom newAlias(String name, String stage, List<VariableExpr> vars) {
		throw new UnsupportedOperationException()
	}

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
