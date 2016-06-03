/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Symbolic Pathfinder (jpf-symbc) is licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package gov.nasa.jpf.symbc.bytecode;


import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.CastExpression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;

import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * Convert double to int
 * ..., value => ..., result
 */
public class D2I extends gov.nasa.jpf.jvm.bytecode.D2I {

  @Override
  public Instruction execute (ThreadInfo th) {
      StackFrame sf = th.getModifiableTopFrame();
	  Expression<?> sym_dval = (Expression<?>) sf.getLongOperandAttr();
		
	  if(sym_dval == null) {
		  //System.out.println("Execute concrete D2I");
		  return super.execute(th); 
	  }
	  else {
        Expression<Double> sym_d = sym_dval.requireAs(BuiltinTypes.DOUBLE);
        CastExpression<Double, Integer> cast = CastExpression.create(sym_d, BuiltinTypes.SINT32);
			
        sf.popLong();
		sf.push(0,false); // for symbolic expressions, the concrete value does not matter
			
		sf.setOperandAttr(cast);
			
		return getNext(th);
		  
	  }
  }
}
