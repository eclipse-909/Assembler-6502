# 6502 Assembler
* Install v2.0.0 from Releases (Windows x86_64 executable)
* Run in the shell:
```shell
/path/to/asm_6502.exe --help
```
* See v1.0.0 for a platform-agnostic text-editor with similar capabilities.
___
## Features
- This assembler doesn't support most instructions in the 6502 ISA. The supported instructions are listed below.
- The output binary is not an actual binary file. It is a sequence of hexadecimal numbers which represent the binary.
- You can find the 6502 emulator that this is meant for here: https://tsiram.com/

Here are the supported instructions:  
| Op Code | Mnemonic | Assembly | Hex |
| -------- | -------- | -------- | -------- |
| A9 | LDA | LDA #\$07 | A9 07 |
| AD | LDA | LDA \$0010 | AD 10 00 |
| 8D | STA | STA \$0010 | 8D 10 00 |
| 8A | TXA | TXA | 8A |
| 98 | TYA | TYA | 98 |
| 6D | ADC | ADC \$0010 | 6D 10 00 |
| A2 | LDX | LDX #\$01 | A2 01 |
| AE | LDX | LDX \$0010 | AE 10 00 |
| AA | TAX | TAX | AA |
| A0 | LDY | LDY #\$04 | A0 04 |
| AC | LDY | LDY \$0010 | AC 10 00 |
| A8 | TAY | TAY | A8 |
| EA | NOP | NOP | EA |
| 00 | BRK | BRK | 00 |
| EC | CPX | CPX \$0010 | EC 10 00 |
| D0 | BNE | BNE \$EF | D0 EF |
| EE | INC | INC \$0021 | EE 21 00 |
| FF | SYS | SYS | FF |
| __ | DAT | DAT \$05 | 05 |

# Syntax
- Extra spacing is ignored.
- Anything after ";" is a comment.
- Not case-sensitive.
- .org \$#### is optional, but must be before any code.
- .org \$#### is the base address. 0x0000 is default if not specified.
- .end is optional, but anything after is ignored.
- .end \$#### is the limit address. This should be specified if you assemble for a system with fixed-size process allocations.
- If an end address is specified, branches will wrap around the limit to branch backwards. Otherwise, they will use two's compliment.
- Use DAT to declare global variables.
- DAT can be followed with hex like \$######... or a string like "Hello, World!".
- DAT strings must be valid UTF-8.
- DAT strings support the escape sequences \\n, \\r, \\\\, and \\".
- DAT strings automatically include a null-terminator.
- Labels must end in ":", only contain letters, numbers, or underscores, and cannot contain spaces or start with a number.
- Hex is the only supported base.
- Addresses are stored little-endian, DAT values are stored as written.
- If you need a label to apply to an operand rather than an opcode,
  you can do "label: +\$##" where ## is the hex number representing the offset from the opcode.
  For example, +\$00 would do nothing, and +\$01 would be the byte after, and so on
- SYStem calls are assumed to work exclusively for the system this assembler was built for.
- See example below for SYS syntax.

Here's example code:
```Assembly
    .ORG $0000
    
    LDX #$01
    LDY #$07
    SYS         ;print the number (7) in the Y-register
    
    SYS four,1  ;print the number (4) in the Y-register
    
    SYS helloWorld,2  ;print the \0-terminated string at helloWorld (uses relative addressing)
    SYS helloWorld,3  ;also print the \0-terminated string at helloWorld (uses absolute addressing)
    BRK
    
helloWorld:
    DAT "Hello, World!"
four:
    DAT $04
    
    .END ;0xFFFF is the highest addressible memory address of the process
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

    .END $00FF ;0x00FF is the highest addressible memory address of the process
```

# Contribution
Feel free to open an issue or PR, but I can't promise I'll look at it any time soon.