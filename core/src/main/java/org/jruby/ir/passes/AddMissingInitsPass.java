package org.jruby.ir.passes;

import java.util.Set;

import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.dataflow.analyses.DefinedVariablesProblem;

public class AddMissingInitsPass extends CompilerPass {
    @Override
    public String getLabel() {
        return "Add Missing Initialization";
    }

    @Override
    public String getShortLabel() {
        return "Add Missing Init.";
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        // Make sure flags are computed
        scope.computeScopeFlags();

        // Find undefined vars
        DefinedVariablesProblem p = new DefinedVariablesProblem(scope);
        p.compute_MOP_Solution();
        Set<Variable> undefinedVars = p.findUndefinedVars();

        // Add inits to entry
        BasicBlock bb = scope.getCFG().getEntryBB();
        for (Variable v : undefinedVars) {
            // System.out.println("Adding missing init for " + v + " in " + scope);

            // Add lvar inits to the end of the BB
            //   (so that scopes are pushed before its vars are updated)
            // and tmpvar inits to the beginning of the BB
            //   (so that if a bad analysis causes an already initialized tmp
            //    to be found uninitialized, this unnecessary init doesn't
            //    clobber an already updated tmp. The entryBB will not have
            //    any loads of lvars, so lvars aren't subject to this problem).
            if (v instanceof LocalVariable) {
                bb.getInstrs().add(new CopyInstr(v, Nil.NIL));
            } else {
                bb.getInstrs().add(0, new CopyInstr(v, Nil.NIL));
            }
        }

        return null;
    }
}
