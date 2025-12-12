package mars.mips.instructions.customlangs;

import mars.*;
import mars.mips.hardware.*;
import mars.mips.instructions.*;
import mars.util.*;

/**
 * RustAssembly
 *
 * A small custom instruction set inspired by the game Rust.
 * Keeps the normal MIPS register file & memory, but defines:
 *
 * - Basic ALU / memory / branch instructions
 * - Unique ops: GRUB, TRIPLED, LOADED, DEEP, PRIM
 */
public class RustAssembly extends CustomAssembly {

    @Override
    public String getName() {
        return "Rust Assembly";
    }

    @Override
    public String getDescription() {
        return "Rust (the game) flavored instructions on top of MIPS-like semantics.";
    }

    @Override
    protected void populate() {

        // 1–5: Basic R-type ALU

        // 1) ADD: rD = rS + rT
        instructionList.add(new BasicInstruction(
                "ADD $t0,$t1,$t2",
                "Add: rd = rs + rt.",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 000001",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands(); // [d, s, t]
                        int s = RegisterFile.getValue(op[1]);
                        int t = RegisterFile.getValue(op[2]);
                        RegisterFile.updateRegister(op[0], s + t);
                    }
                }
        ));

        // 2) SUB: rD = rS - rT
        instructionList.add(new BasicInstruction(
                "SUB $t0,$t1,$t2",
                "Subtract: rd = rs - rt.",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 000010",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();
                        int s = RegisterFile.getValue(op[1]);
                        int t = RegisterFile.getValue(op[2]);
                        RegisterFile.updateRegister(op[0], s - t);
                    }
                }
        ));

        // 3) AND: bitwise AND
        instructionList.add(new BasicInstruction(
                "AND $t0,$t1,$t2",
                "Bitwise AND: rd = rs & rt.",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 000011",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();
                        int s = RegisterFile.getValue(op[1]);
                        int t = RegisterFile.getValue(op[2]);
                        RegisterFile.updateRegister(op[0], s & t);
                    }
                }
        ));

        // 4) OR: bitwise OR
        instructionList.add(new BasicInstruction(
                "OR $t0,$t1,$t2",
                "Bitwise OR: rd = rs | rt.",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 000100",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();
                        int s = RegisterFile.getValue(op[1]);
                        int t = RegisterFile.getValue(op[2]);
                        RegisterFile.updateRegister(op[0], s | t);
                    }
                }
        ));

        // 5) XOR: bitwise XOR
        instructionList.add(new BasicInstruction(
                "XOR $t0,$t1,$t2",
                "Bitwise XOR: rd = rs ^ rt.",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 000101",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();
                        int s = RegisterFile.getValue(op[1]);
                        int t = RegisterFile.getValue(op[2]);
                        RegisterFile.updateRegister(op[0], s ^ t);
                    }
                }
        ));
        
        // 6–9: Basic I-type + branch

        // 6) ADDI: rT = rS + imm
        instructionList.add(new BasicInstruction(
                "ADDI $t0,$t1,100",
                "Add immediate: rt = rs + imm (sign-extended).",
                BasicInstructionFormat.I_FORMAT,
                "000001 sssss fffff tttttttttttttttt",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();  // [t, s, imm]
                        int s = RegisterFile.getValue(op[1]);
                        int imm = op[2] << 16 >> 16;  // sign extend
                        RegisterFile.updateRegister(op[0], s + imm);
                    }
                }
        ));

        // 7) LD: rT = MEM[rS + offset]
        instructionList.add(new BasicInstruction(
                "LD $t0,100($t1)",
                "Load word: rt = MEM[rs + offset].",
                BasicInstructionFormat.I_FORMAT,
                "000010 sssss fffff tttttttttttttttt",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();   // [t, base, offset]
                        int base = RegisterFile.getValue(op[1]);
                        int off  = op[2] << 16 >> 16;  // sign extend
                        int addr = base + off;
                        try {
                            int w = Globals.memory.getWord(addr);
                            RegisterFile.updateRegister(op[0], w);
                        } catch (AddressErrorException e) {
                            throw new ProcessingException(st, e);
                        }
                    }
                }
        ));

        // 8) ST: MEM[rS + offset] = rT
        instructionList.add(new BasicInstruction(
                "ST $t0,100($t1)",
                "Store word: MEM[rs + offset] = rt.",
                BasicInstructionFormat.I_FORMAT,
                "000011 sssss fffff tttttttttttttttt",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();   // [t, base, offset]
                        int base = RegisterFile.getValue(op[1]);
                        int off  = op[2] << 16 >> 16;
                        int addr = base + off;
                        int val  = RegisterFile.getValue(op[0]);
                        try {
                            Globals.memory.setWord(addr, val);
                        } catch (AddressErrorException e) {
                            throw new ProcessingException(st, e);
                        }
                    }
                }
        ));

        // 9) BEQ: branch if equal (two regs + target)
        instructionList.add(new BasicInstruction(
                "BEQ $t0,$t1,100",
                "Branch if equal: if (rs == rt) branch to label/offset.",
                BasicInstructionFormat.I_BRANCH_FORMAT,
                "000100 sssss ttttt ffffffffffffffff",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();  // [s, t, branch target]
                        int s = RegisterFile.getValue(op[0]);
                        int t = RegisterFile.getValue(op[1]);
                        if (s == t) {
                            Globals.instructionSet.processBranch(op[2]);
                        }
                    }
                }
        ));

        // 10: GRUB – gather small immediate

        // 10) GRUB: rT = imm & 0xFF
        instructionList.add(new BasicInstruction(
                "GRUB $t0,100",
                "GRUB: load tiny constant (0–255) into rt.",
                BasicInstructionFormat.I_FORMAT,
                "010000 fffff 00000 ssssssssssssssss",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();  // [t, imm]
                        int imm = op[1] & 0xFF;       // clamp to low 8 bits
                        RegisterFile.updateRegister(op[0], imm);
                    }
                }
        ));

        // 11) TRIPLED: rD = rS * 3
        instructionList.add(new BasicInstruction(
                "TRIPLED $t0,$t1",
                "TRIPLED: rd = 3 * rs.",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss 00000 fffff 00000 100000",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();  // [d, s]
                        int v = RegisterFile.getValue(op[1]);
                        RegisterFile.updateRegister(op[0], v * 3);
                    }
                }
        ));

        // 12) LOADED: like LD but sets MSB (armed)
        instructionList.add(new BasicInstruction(
                "LOADED $t0,100($t1)",
                "LOADED: rt = MEM[rs + offset] with MSB set (0x80000000).",
                BasicInstructionFormat.I_FORMAT,
                "010001 sssss fffff tttttttttttttttt",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();  // [t, base, offset]
                        int base = RegisterFile.getValue(op[1]);
                        int off  = op[2] << 16 >> 16;
                        int addr = base + off;
                        try {
                            int w = Globals.memory.getWord(addr);
                            w |= 0x80000000;
                            RegisterFile.updateRegister(op[0], w);
                        } catch (AddressErrorException e) {
                            throw new ProcessingException(st, e);
                        }
                    }
                }
        ));
        
        // 13) DEEP: rd = bit-reverse(rs)
        instructionList.add(new BasicInstruction(
                "DEEP $t0,$t1",
                "DEEP: bit-reverse rs into rd.",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss 00000 fffff 00000 100001",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();  // [d, s]
                        int v = RegisterFile.getValue(op[1]);
                        int reversed = Integer.reverse(v); // reverse all 32 bits
                        RegisterFile.updateRegister(op[0], reversed);
                    }
                }
        ));
        
        // 14) PRIM: rd = 1 if rs is prime, else 0
        instructionList.add(new BasicInstruction(
                "PRIM $t0,$t1",
                "PRIM: rd = 1 if rs is prime, else 0.",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss 00000 fffff 00000 100010",
                new SimulationCode() {
                    public void simulate(ProgramStatement st) throws ProcessingException {
                        int[] op = st.getOperands();  // [d, s]
                        int v = RegisterFile.getValue(op[1]);
                        int isPrime = isPrime(v) ? 1 : 0;
                        RegisterFile.updateRegister(op[0], isPrime);
                    }

                    private boolean isPrime(int n) {
                        if (n <= 1) return false;
                        if (n == 2 || n == 3) return true;
                        if ((n & 1) == 0) return false;
                        int limit = (int)Math.sqrt(n);
                        for (int i = 3; i <= limit; i += 2) {
                            if (n % i == 0) return false;
                        }
                        return true;
                    }
                }
        ));
    }
}
