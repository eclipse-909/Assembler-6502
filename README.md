# Assembler-6502 Text Editor
- This is a very rudimentary text editor for educational purposes which allows you to write 6502 assembly.  
- This is an editor and an assembler. It doesn't support most instructions in the 6502 ISA. The supported instructions are listed below.  
- If you successfully assemble a program, it will provide the hex dump which you can use in an emulator or something.  
- This editor doesn't provide much feedback or debugging except when you attempt to assemble.  
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
| __ | DAT | DAT $05 | 05 |

# Download
You can find the JAR file here: https://github.com/eclipse-909/Assembler-6502/releases/tag/Latest

# Syntax
- Extra spacing is ignored
- Anything after ";" is a comment
- Not case-sensitive
- Must start with .org $####, but can only be used once
- .end is optional, but anything after is ignored
- Use DAT to declare variables
- Labels must end in ":" and cannot contain spaces or start with a number
- Hex is the only supported base
- Addresses are stored little-endian, DAT values are stored as written
- If you need a label to apply to an operand rather than an opcode,
you can do "label: +$##" where ## is the hex number representing the offset from the opcode.
For example, +$00 would do nothing, and +$01 would be the byte after, and so on

Here's example code:
```Assembly
    .ORG $0000
    
    LDX #$03
    SYS helloWorld  ;print the \0-terminated string at helloWorld
    BRK
    
helloWorld:
    DAT $48656C6C6F20576F726C642100
    
    .END
```

```Assembly
    .ORG $0000

low_byte: +$01   ;label points to 08
    LDX $0008    ;load 02 into X

high_byte: +$02  ;label points to 00
    LDA $0007    ;load 01 into A

second_byte: +$01 ;label points to 02
    DAT $010203

    STA low_byte ;store 01 where the 08 was

    BRK

    .END
```

# Contribution
Feel free to open an issue or PR, but I can't promise I'll look at it any time soon.