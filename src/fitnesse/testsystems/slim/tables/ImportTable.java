// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.testsystems.slim.tables;

import static util.ListUtility.list;

import java.util.ArrayList;
import java.util.List;

import fitnesse.slim.instructions.ImportInstruction;
import fitnesse.slim.instructions.Instruction;
import fitnesse.testsystems.slim.SlimTestContext;
import fitnesse.testsystems.slim.Table;

public class ImportTable extends SlimTable {
  public ImportTable(Table table, String id, SlimTestContext testContext) {
    super(table, id, testContext);
  }

  protected String getTableType() {
    return "import";
  }

  public List<Instruction> getInstructions() throws SyntaxError {
    int rows = table.getRowCount();
    List<Instruction> instructions = new ArrayList<Instruction>(rows);
    if (rows < 2)
      throw new SyntaxError("Import tables must have at least two rows.");

    for (int row = 1; row < rows; row++) {
      String importString = table.getCellContents(0, row);
      if (importString.length() > 0) {
        Instruction importInstruction = new ImportInstruction(makeInstructionTag(), importString);
        instructions.add(importInstruction);
      }
    }
    return instructions;
  }

}