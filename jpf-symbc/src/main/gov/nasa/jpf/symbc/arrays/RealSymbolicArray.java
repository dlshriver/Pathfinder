package gov.nasa.jpf.symbc.arrays;

import java.util.HashMap;
import java.util.Map;

import gov.nasa.jpf.symbc.numeric.ConstraintExpressionVisitor;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.arrays.PreviousIntegerArray;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.RealExpression;


public class RealSymbolicArray extends ArrayExpression {
    private String name;
    public String solution = "UNDEFINED";
    public int slot;
    // Indicates the previous ArrayExpression, as well as the index and value
    // when we store something in the array
    public PreviousRealArray previous = null;
    public Map<String, SymbolicRealValueAtIndex> realValAt = null;


    public RealSymbolicArray(int size, int slot) {
        super();
        this.length = new IntegerConstant(size);
        this.slot = slot;
    }

    public RealSymbolicArray(int n, String name, int slot) {
        super();
        this.name = name;
        this.length = new IntegerConstant(n);
        this.slot = slot;
    }

    public RealSymbolicArray(IntegerExpression n, String name, int slot) {
        super();
        this.name = name;
        this.length = n;
        this.slot = slot;
    }

    public RealSymbolicArray(PreviousRealArray previous) {
        super();
        this.length = previous.ae.length;
        String newName = previous.ae.name;
        if (newName.indexOf("!") == -1) {
            newName = newName+ "!1";
        } else {
            int aux = Integer.parseInt(newName.substring(newName.indexOf("!") + 1));
            newName = newName.substring(0, newName.indexOf("!") +1) + (aux + 1);
        }
        this.name = newName;
        this.slot = previous.ae.slot;
        this.previous = previous;
    }


    public IntegerExpression __length() {
        return length;
    }
    
    public int getSlot() {
        return slot;
    }

    public String getName() {
        return (name!=null) ? name : "ARRAY_" + hashCode();
   }

    public String solution() {
        return solution;
    }

    public String stringPC() {
        return (name != null) ? name : "ARRAY_" + hashCode();
    }

    public void accept(ConstraintExpressionVisitor visitor) {
        visitor.preVisit(this);
        visitor.postVisit(this);
    }

    public void getVarsVals(Map<String,Object> varsVals) {
        varsVals.put(name, solution);
    }

    public SymbolicRealValueAtIndex getRealVal(IntegerExpression index) {
        if (realValAt == null) {
            realValAt = new HashMap<String, SymbolicRealValueAtIndex>();
        }
        SymbolicRealValueAtIndex result = realValAt.get(index.toString());
        if (result == null) {
            result = new SymbolicRealValueAtIndex(this, index);
            realValAt.put(index.toString(), result);
        }
        return result;
    }
}