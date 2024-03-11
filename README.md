# WORK IN PROGRESS
# Assembler-6502 Text Editor
- This is a very rudimentary text editor for educational purposes which allows you to write 6502 assembly.  
- This is an editor and an assembler. It doesn't support most instructions in the 6502 ISA. The supported instructions are listed below.  
- If you successfully assemble a program, it will provide the hex dump which you can use in an emulator or something.  
- This editor doesn't provide any feedback or debugging except when you attempt to assemble.  
- You can find the 6502 emulator that this is meant for here: https://tsiram.com/  

Here are the supported instructions:  
| Op Code | Mnemonic | Assembly | Hex |
| -------- | -------- | -------- | -------- |
| A9 | LDA | LDA #$07 | A9 07 |
| AD | LDA | LDA $0010 | AD 10 00 |
| 8D | STA | STA $0010 | 8D 10 00 |
| 8A | TXA | TXA | 8A |
| 98 | TYA | TYA | 98 |
| 6D | ADC | ADC $0010 | 6D 10 00 |
| A2 | LDX | LDX #$01 | A2 01 |
| AE | LDX | LDX $0010 | AE 10 00 |
| AA | TAX | TAX | AA |
| A0 | LDY | LDY #$04 | A0 04 |
| AC | LDY | LDY $0010 | AC 10 00 |
| A8 | TAY | TAY | A8 |
| EA | NOP | NOP | EA |
| 00 | BRK | BRK | 00 |
| EC | CPX | CPX $0010 | EC 10 00 |
| D0 | BNE | BNE $EF | D0 EF |
| EE | INC | INC $0021 | EE 21 00 |
| FF | SYS | SYS | FF |
| FF | SYS | SYS $0010 | FF 10 00 |
