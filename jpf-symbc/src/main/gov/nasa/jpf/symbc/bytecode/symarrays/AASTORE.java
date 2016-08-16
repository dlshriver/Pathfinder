/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
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


// author aymeric fromherz aymeric.fromherz@ens.fr 

package gov.nasa.jpf.symbc.bytecode.symarrays;

import gov.nasa.jpf.symbc.SymbolicInstructionFactory;

import gov.nasa.jpf.symbc.arrays.ArrayExpression;
import gov.nasa.jpf.symbc.arrays.StoreExpression;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.vm.ArrayIndexOutOfBoundsExecutiveException;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;


/**
 * Store into reference array
 * ..., arrayref, index, value  => ...
 */
public class AASTORE extends gov.nasa.jpf.jvm.bytecode.AASTORE {

	 @Override
	  public Instruction execute (ThreadInfo ti) {
		 
          IntegerExpression indexAttr = null;
          ArrayExpression arrayAttr = null;
          StackFrame frame = ti.getModifiableTopFrame();
          ChoiceGenerator<?> cg;
          boolean condition;
		  int arrayRef = peekArrayRef(ti); // need to be polymorphic, could be LongArrayStore
        
          // Retrieve the array expression if it was previously in the pathcondition, and store it as an array attr
          PCChoiceGenerator temp_cg = (PCChoiceGenerator)ti.getVM().getLastChoiceGeneratorOfType(PCChoiceGenerator.class);
          if (temp_cg != null) {
              if (temp_cg.getCurrentPC().arrayExpressions.containsKey(ti.getElementInfo(ti.getModifiableTopFrame().peek(2)).toString())) {
                  ti.getModifiableTopFrame().setOperandAttr(2, temp_cg.getCurrentPC().arrayExpressions.get(ti.getElementInfo(ti.getModifiableTopFrame().peek(2)).toString()));
              }
          }

          if (peekArrayAttr(ti) == null || !(peekArrayAttr(ti) instanceof ArrayExpression)) {
              // In this case, the array isn't symbolic
              if (peekIndexAttr(ti) == null || !(peekIndexAttr(ti) instanceof IntegerExpression)) {
                  // The symbolic object was concretized during AALOAD or ALOAD, so nothing is symbolic here
                  return super.execute(ti);
              } else {
                  // The array is not symbolic, but the index is.
                  // We try to store the object in each possible slot
                  if (arrayRef == MJIEnv.NULL) {
                      return ti.createAndThrowException("java.lang.NullPointerException");
                  }
                  ElementInfo arrayInfo = ti.getElementInfo(arrayRef);
                  if (!ti.isFirstStepInsn()) { // first time around
                     cg = new PCChoiceGenerator(arrayInfo.arrayLength() +2);
                     ((PCChoiceGenerator) cg).setOffset(this.position);
                     ((PCChoiceGenerator) cg).setMethodName(this.getMethodInfo().getFullName());
                     ti.getVM().setNextChoiceGenerator(cg);
                     return this;
                  } else { // this is what really returns results
                    cg = ti.getVM().getChoiceGenerator();
                    assert (cg instanceof PCChoiceGenerator) : "expected PCChoiceGenerator, got: " + cg;
                  }

                  PathCondition pc;
                  ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);

                  if (prev_cg == null)
                      pc = new PathCondition();
                  else
                      pc = ((PCChoiceGenerator)prev_cg).getCurrentPC();

                  assert pc != null;

                  indexAttr = (IntegerExpression)peekIndexAttr(ti); // We know that the index is here symbolic
              
                  assert (indexAttr != null) : "indexAttr shouldn't be null in AASTORE instruction";

                  int currentChoice = (Integer)cg.getNextChoice();

                  if (currentChoice < arrayInfo.arrayLength()) {
                      pc._addDet(Comparator.EQ, indexAttr, new IntegerConstant(currentChoice));
                      if (pc.simplify()) { // We can store at this index
                        int value = frame.peek();
                        arrayRef = frame.peek(2);
                        ElementInfo eiArray = ti.getModifiableElementInfo(arrayRef);
                        eiArray.setReferenceElement(currentChoice, value);

                        frame.pop(3);
                        return getNext(ti);
                      } else {
                        ti.getVM().getSystemState().setIgnored(true);
                        return getNext(ti);
                     }
                  } else if (currentChoice == arrayInfo.arrayLength()) {
                      pc._addDet(Comparator.GE, indexAttr, new IntegerConstant(arrayInfo.arrayLength()));
                      if (pc.simplify()) { // satisfiable
                          ((PCChoiceGenerator) cg).setCurrentPC(pc);
                          return ti.createAndThrowException("java.lang.ArrayIndexOutOfBoundsException", "index greater than array bounds");
                      } else {
                        ti.getVM().getSystemState().setIgnored(true);
                        return getNext(ti);
                      }
                  } else {
                      pc._addDet(Comparator.LT, indexAttr, new IntegerConstant(0));
                      if (pc.simplify()) {
                          ((PCChoiceGenerator) cg).setCurrentPC(pc);
                          return ti.createAndThrowException("java.lang.ArrayIndexOutOfBoundsException", "index smaller than array bounds");
                      } else {
                          ti.getVM().getSystemState().setIgnored(true);
                          return getNext(ti);
                      }
                  }
              }
          }


          if (!ti.isFirstStepInsn()) { // first time around
              cg = new PCChoiceGenerator(3);
              ((PCChoiceGenerator) cg).setOffset(this.position);
              ((PCChoiceGenerator) cg).setMethodName(this.getMethodInfo().getFullName());
              ti.getVM().setNextChoiceGenerator(cg);
              return this;
          } else { // this is what really returns results
              cg = ti.getVM().getChoiceGenerator();
              assert (cg instanceof PCChoiceGenerator) : "expected PCChoiceGenerator, got: " + cg;
          }

          PathCondition pc;
          ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);

          if (prev_cg == null)
              pc = new PathCondition();
          else
              pc = ((PCChoiceGenerator)prev_cg).getCurrentPC();

          assert pc != null;

          if (peekIndexAttr(ti) == null || !(peekIndexAttr(ti) instanceof IntegerExpression)) {
              int index = ti.getTopFrame().peek(1);
              indexAttr = new IntegerConstant(index);
          } else {
              indexAttr = (IntegerExpression)peekIndexAttr(ti);
          }
          assert (indexAttr != null) : "indexAttr shouldn't be null in AASTORE instruction";

          if (peekArrayAttr(ti) == null || !(peekArrayAttr(ti) instanceof ArrayExpression)) {
		      if (arrayRef == MJIEnv.NULL) {
		          return ti.createAndThrowException("java.lang.NullPointerException");
		      } 
              // In this case the array isn't symbolic, and we checked earlier that the index was symbolic
              ElementInfo arrayInfo = ti.getElementInfo(arrayRef);
              // We need to add information about the type of the elements in the array as well
              // We should add the constraints about the elements of the array here
              // We should not end here, case handled previously
              throw new RuntimeException("constant object array with symbolic index not implemented");
          } else {
              arrayAttr = (ArrayExpression)peekArrayAttr(ti);
              if (pc.arrayExpressions.containsKey(arrayAttr.getRootName())) {
                  arrayAttr = (ArrayExpression)pc.arrayExpressions.get(arrayAttr.getRootName());
              }
          }
          assert (arrayAttr != null) : "arrayAttr shouldn't be null in AASTORE instruction";


	      if ((Integer)cg.getNextChoice() == 1) { // check bounds of the index
              pc._addDet(Comparator.GE, indexAttr, arrayAttr.length);
              if (pc.simplify()) { // satisfiable
                  ((PCChoiceGenerator) cg).setCurrentPC(pc);
                  return ti.createAndThrowException("java.lang.ArrayIndexOutOfBoundsException", "index greater than array bounds");
              } else {
                  ti.getVM().getSystemState().setIgnored(true);
                  return getNext(ti);
              }
          } else if ((Integer)cg.getNextChoice() == 2) {
              pc._addDet(Comparator.LT, indexAttr, new IntegerConstant(0));
              if (pc.simplify()) { // satisfiable
                  ((PCChoiceGenerator) cg).setCurrentPC(pc);
                  return ti.createAndThrowException("java.lang.ArrayIndexOutOfBoundsException", "index smaller than array bounds");
              } else {
                  ti.getVM().getSystemState().setIgnored(true);
                  return getNext(ti);
              }
          } else {
              pc._addDet(Comparator.LT, indexAttr, arrayAttr.length);
              pc._addDet(Comparator.GE, indexAttr, new IntegerConstant(0));
              if (pc.simplify()) { // satisfiable
                  ((PCChoiceGenerator) cg).setCurrentPC(pc);

                  int value = frame.pop();
                  IntegerExpression sym_value = new IntegerConstant(value);
                  // We create a new arrayAttr, and inherits information from the previous attribute
                  ArrayExpression newArrayAttr = new ArrayExpression(arrayAttr);
                  frame.pop(2); // We pop the array and the index

                  StoreExpression se = new StoreExpression(arrayAttr, indexAttr, sym_value);
                  pc._addDet(Comparator.EQ, se, newArrayAttr);
                  pc.arrayExpressions.put(newArrayAttr.getRootName(), newArrayAttr);

                  return getNext(ti);
              } else {
                  ti.getVM().getSystemState().setIgnored(true);
                  return getNext(ti);
              }
          }
      }
}