use {
	std::{
		path::PathBuf,
		process,
		fs::File,
		io::{Write, BufReader, BufRead},
		collections::HashMap,
		str::FromStr
	},
	clap::{arg, command, value_parser, ArgAction, ArgMatches},
	once_cell::sync::OnceCell,
	regex::Regex
};

static QUIET: OnceCell<bool> = OnceCell::new();
const IMD_VAL: &str = r"^#\$[0-9a-fA-F]{2}$";
const ADDR_LABEL: &str = r"^((\$[0-9a-fA-F]{4})|([a-zA-Z_]+[a-zA-Z0-9_]*))$";
const LABEL: &str = r"^[a-zA-Z_]+[a-zA-Z0-9_]*$";
const ADDR: &str = r"^\$[0-9a-fA-F]{4}$";
const QUOTES: &str = r#"^"[^"]*"$"#;

///Prints the formatted string if !QUIET.
macro_rules! log {
    ($($arg:tt)*) => {
        if !*QUIET.get().unwrap_or(&false) {
            println!($($arg)*);
        }
    };
}

///Prints the formatted string if !QUIET. Prepends "Warning: " to the string.
macro_rules! warn {
    ($($arg:tt)*) => {
        if !*QUIET.get().unwrap_or(&false) {
            eprintln!("Warning: {}", format!($($arg)*));
        }
    };
}

///Prints the formatted string if !QUIET. Prepends "Error: " to the string.
macro_rules! error {
    ($($arg:tt)*) => {
        if !*QUIET.get().unwrap_or(&false) {
            eprintln!("Error: {}", format!($($arg)*));
        }
    };
}

///First param is the string to be parsed.
///Second param is the prefix to be stripped.
///Third param is the numeric data type to be parsed into.
macro_rules! parse_hex {
    ($input:expr, $prefix:expr, $type:ty) => {
        $input
            .strip_prefix($prefix)
            .and_then(|hex_str| <$type>::from_str_radix(hex_str, 16).ok())
    };
}

macro_rules! parse_string {
    ($input:expr) => {
	    $input[1..$input.len() - 1]
			.replace(r#"\\"#, "\\") // Replace double backslash with a single backslash
			.replace(r#"\n"#, "\n") // Replace \n with a newline
			.replace(r#"\r"#, "\r") // Replace \r with a carriage return
			.replace(r#"\""#, "\"")
    };
}

///First variant: token: String (1 byte, 2 bytes, or label), hex_dump: vector, opcode for immediate addressing, opcode for absolute addressing, labels: hashmap.
///Second variant: token: String (1 byte, 2 bytes, or label), hex_dump: vector, opcode for absolute addressing, labels: hashmap.
macro_rules! match_operand {
    ($token:expr, $hex_dump:expr, $opcode_absolute:expr, $labels:expr, $addr:expr) => {{
        if Regex::new(ADDR).unwrap().is_match($token) {
            // Absolute address
            if let Some(operand) = parse_hex!($token, "$", u16) {
	            $hex_dump.push($opcode_absolute);
		        let v: [u8; 2] = operand.to_le_bytes();
		        $hex_dump.append(&mut v.to_vec());
	            $addr += 3;
	            true
            } else {
	            false
            }
        } else if Regex::new(LABEL).unwrap().is_match($token) {
            // Absolute label
            if let Some((operand, _)) = $labels.get($token) {
	            $hex_dump.push($opcode_absolute);
	            let v: [u8; 2] = operand.to_le_bytes();
	            $hex_dump.append(&mut v.to_vec());
	            $addr += 3;
	            true
            } else {
                false
            }
        } else {
	        false
        }
    }};

    ($token:expr, $hex_dump:expr, $opcode_immediate:expr, $opcode_absolute:expr, $labels:expr, $addr:expr) => {{
        if Regex::new(IMD_VAL).unwrap().is_match($token) {
            // Immediate value
            if let Some(operand) = parse_hex!($token, "#$", u8) {
	            $hex_dump.push($opcode_immediate);
                $hex_dump.push(operand);
	            $addr += 2;
	            true
            } else {
                false
            }
        } else {
            match_operand!($token, $hex_dump, $opcode_absolute, $labels, $addr)
        }
    }};
}

fn main() {
	//get arguments
	let matches: ArgMatches = command!()
		.arg(arg!(-i --input <FILE> "Specify the .asm6502 input file path (include the extension).")
			.required(true)
			.value_parser(value_parser!(PathBuf))
		)
		.arg(arg!(-o --output <FILE> "Specify the .exe6502 output file path (do not include the extension).")
			.required(false)
			.default_value("a.exe6502")
			.value_parser(value_parser!(PathBuf))
		)
		.arg(arg!(-q --quiet "Hide assembler output.")
			.required(false)
			.action(ArgAction::SetTrue)
		)
		.arg(arg!(-x --prefix "Prefix each byte in the output binary with 0x.")
			.required(false)
			.action(ArgAction::SetTrue)
		)
		.arg(arg!(-s --separator <STRING> "Separator between bytes in the output binary.")
			.required(false)
			.default_value(" ")
		)
		.get_matches();
	
	//set quiet
	QUIET.set(matches.get_flag("quiet")).unwrap();
	
	//get path buffers
	let Some(input_path) = matches.get_one::<PathBuf>("input") else {
		error!("Input file is required.");
		process::exit(1);
	};
	let Some(output_path) = matches.get_one::<PathBuf>("output") else {
		error!("Output file is required.");
		process::exit(1);
	};
	let mut output_path: PathBuf = output_path.to_owned();
	
	//verify input file
	if !input_path.exists() {
		error!("Error: Input file does not exist.");
		process::exit(1);
	}
	if !input_path.is_file() {
		error!("Error: Input file is not a file.");
		process::exit(1);
	}
	
	//create output file
	output_path.set_extension("exe6502");
	if File::create(output_path.clone()).is_err() {
		error!("Failed to open output file.");
		process::exit(1);
	}
	
	//read input file to string
	let output_content: String;
	if let Ok(input_file) = File::open(input_path) {
		let mut assembly_successful: bool = true;
		//symbol -> address
		let mut labels: HashMap<String, (u16, usize)> = HashMap::new();
		let mut org_addr: Option<u16> = None;
		let mut end_addr: Option<u16> = None;
		let mut addr: u16 = 0x0000;
		let mut lines: Vec<(Vec<String>, usize)> = Vec::new();
		for (line_num, line_res) in BufReader::new(input_file).lines().enumerate() {
			let Ok(mut line) = line_res else {
				error!("Failed to read input file.");
				process::exit(1);
			};
			line = line.trim().to_string();
			if line.is_empty() {continue;}
			
			//split line by white space and commas, ignore comments, and preserve strings with double quotes
			let mut tokens: Vec<String> = Regex::new(r#"(?x)
		        ("[^"]*")        # Match anything inside double quotes, including the quotes
		        |                # OR
		        ([^;\s]+)       # Match sequences that are not whitespace or semicolons
		        |                # OR
		        (;.*$)           # Match semicolon and everything after (comment)
		    "#).unwrap()
				.captures_iter(line.as_str())
				.filter_map(|cap| {
					if let Some(quoted) = cap.get(1) {
						Some(quoted.as_str().to_string()) // Quoted text with quotes
					} else if let Some(unquoted) = cap.get(2) {
						Some(unquoted.as_str().to_string()) // Unquoted text
					} else {
						None // Skip commas and comments
					}
				})
				.collect();
			
			if tokens.len() == 0 {continue;}
			
			//check for .org
			if org_addr.is_none() && tokens[0].to_lowercase() != ".org" {
				warn!("Base address is not set. 0x0000 will be used by default. Consider including '.ORG $0000' prior to your code.");
				org_addr = Some(0x0000);
			}
			if tokens[0].to_lowercase() == ".org" {
				if org_addr.is_some() {
					error!("Cannot set org address more than once or after any code. Line {}.", line_num + 1);
					assembly_successful = false;
					continue;
				}
				if tokens.len() == 1 {
					error!("Syntax error, please provide address. Must be of format '.ORG $0000'. Line {}.", line_num + 1);
					assembly_successful = false;
					continue;
				} else if tokens.len() > 2 {
					error!("Syntax error, unnecessary token. Must be of format '.ORG $0000'. Line {}.", line_num + 1);
					assembly_successful = false;
					continue;
				}
				if let Some(hex_num) = parse_hex!(tokens[1], "$", u16) {
					org_addr = Some(hex_num);
					addr = hex_num;
					continue;
				} else {
					error!("Syntax error, could not parse address. Must be of format '$0000'. Line {}.", line_num + 1);
					assembly_successful = false;
					continue;
				};
			}
			
			//check for .end
			if tokens[0].to_lowercase() == ".end" {
				if tokens.len() == 2 {
					if let Some(addr) = parse_hex!(tokens[1], "$", u16) {
						end_addr = Some(addr);
					} else {
						error!("Syntax error, invalid address. Must be of format '.END' or '.END $FFFF'. Line {}.", line_num + 1);
						assembly_successful = false;
						continue;
					}
				} else if tokens.len() > 2 {
					error!("Syntax error, unnecessary token. Must be of format '.END' or '.END $FFFF'.\nIf an address is not specified, branches that use labels will use two's compliment to handle backwards branching.\nIf an address is specified, it will be used as the logical limit address for a fixed-size process allocation, and branches that use labels will wrap around it.\nLine {}.", line_num + 1);
					assembly_successful = false;
					continue;
				}
				break;
			}
			
			//check for label
			if Regex::new(r"^[a-zA-Z_]+[a-zA-Z0-9_]*:$").unwrap().is_match(tokens[0].as_str()) {
				let mut offset: u16 = 0;
				let label: String = tokens[0].strip_suffix(":").unwrap().to_string();
				if labels.contains_key(&label) {
					error!("Duplicate label found. Original label on line {}. Duplicate label on line {}.", labels.get(&label).unwrap().1, line_num + 1);
					assembly_successful = false;
					continue;
				}
				if tokens.len() == 2 && Regex::new(r"^\+\$[a-f0-9]{2}$").unwrap().is_match(tokens[1].as_str()) {
					let Some(num) = parse_hex!(tokens[1], "+$", u8) else {
						error!("Syntax error, could not parse address offset. Must be of format 'label: +$00'. Line {}.", line_num + 1);
						assembly_successful = false;
						continue;
					};
					offset = num as u16;
				} else if tokens.len() > 2 {
					error!("Syntax error, unnecessary token. Must be of format 'label: +$00'. Line {}.", line_num + 1);
					assembly_successful = false;
					continue;
				}
				labels.insert(tokens[0].strip_suffix(":").unwrap().to_string(), (addr + offset, line_num + 1));
				continue;
			} else {
				tokens[0] = tokens[0].to_lowercase();
			}
			
			//parse instruction (first pass to verify opcodes and operand count)
			let instruction_len: u8;
			match tokens[0].as_str() {
				"lda" | "ldx" | "ldy" => {
					if tokens.len() == 2 {
						if Regex::new(IMD_VAL).unwrap().is_match(tokens[1].as_str()) {
							instruction_len = 2;
						} else if Regex::new(ADDR_LABEL).unwrap().is_match(tokens[1].as_str()) {
							instruction_len = 3;
						} else {
							error!("Syntax error, invalid operand. Operand must be written with format '#$00' or '$0000' or a label. Line {}.", line_num + 1);
							assembly_successful = false;
							continue;
						}
					} else {
						error!("Syntax error, unnecessary token. Operand must be written with format '#$00' or '$0000' or a label. Line {}.", line_num + 1);
						assembly_successful = false;
						continue;
					}
				}
				"sta" | "adc" | "cpx" | "inc" => {
					if tokens.len() == 2 {
						if Regex::new(ADDR_LABEL).unwrap().is_match(tokens[1].as_str()) {
							instruction_len = 3;
						} else {
							error!("Syntax error, invalid operand. Operand must be written with format '$0000' or a label. Line {}.", line_num + 1);
							assembly_successful = false;
							continue;
						}
					} else {
						error!("Syntax error, unnecessary token. Operand must be written with format '$0000' or a label. Line {}.", line_num + 1);
						assembly_successful = false;
						continue;
					}
				}
				"bne" => {
					if tokens.len() == 2 {
						if Regex::new(r"^((\$[0-9a-fA-F]{2})|([a-zA-Z_]+[a-zA-Z0-9_]*))$").unwrap().is_match(tokens[1].as_str()) {
							instruction_len = 2;
						} else {
							error!("Syntax error, invalid operand. Operand must be written with format '$00' or a label. Line {}.", line_num + 1);
							assembly_successful = false;
						continue;
						}
					} else {
						error!("Syntax error, unnecessary token. Operand must be written with format '$00' or a label. Line {}.", line_num + 1);
						assembly_successful = false;
						continue;
					}
				}
				"txa" | "tya" | "tax" | "tay" | "nop" | "brk" => {
					instruction_len = 1;
					if tokens.len() > 1 {
						error!("Syntax error, unnecessary token. Instruction requires no operands. Line {}.", line_num + 1);
						assembly_successful = false;
						continue;
					}
				}
				"sys" => {
					if tokens.len() == 1 {
						instruction_len = 1;
					} else {
						if Regex::new(r"^((\$[0-9a-fA-F]{4})|([a-zA-Z_]+[a-zA-Z0-9_]*))\s*,\s*[1-3]$").unwrap().is_match(tokens[1..].join("").as_str()) {
							let mut t: Vec<String> = tokens[1].split(",").map(|s| s.to_string()).collect();
							tokens.drain(1..);
							tokens.append(&mut t);
							if Regex::new(r"^[1-3]$").unwrap().is_match(tokens[2].as_str()) {
								let num: u8 = u8::from_str(tokens[2].as_str()).unwrap();
								//num == 1
								//LDX #$01
								//LDY label or absolute address
								//SYS
								//instruction_len = 6
								
								//num == 2
								//LDX #$02
								//LDY relative address
								//SYS
								//instruction_len = 5
								
								//num == 3
								//LDX #$03
								//SYS label or absolute address
								//instruction_len = 5
								if num == 1 {
									instruction_len = 6;
								} else {
									instruction_len = 5;
								}
							} else {
								error!("Bad value. When using a label with a system call, you must set the X register to 1-3. Line {}.", line_num + 1);
								assembly_successful = false;
								continue;
							}
						} else {
							error!("Syntax error, unrecognized tokens. SYS must be formatted as 'SYS' or 'SYS label,1-3' or 'SYS $0000,1-3'. Line {}.", line_num + 1);
							assembly_successful = false;
							continue;
						}
					}
				}
				"dat" => {
					if tokens.len() == 2 {
						if Regex::new(r"^\$([0-9a-zA-Z]{2})+$").unwrap().is_match(tokens[1].as_str()) {
							instruction_len = (tokens[1].len() as u8 - 1) / 2;
						} else if Regex::new(QUOTES).unwrap().is_match(tokens[1].as_str()) {
							instruction_len = parse_string!(tokens[1]).len() as u8 + 1;//+1 for null-terminator
						} else {
							error!("Syntax error, unrecognized token. Data must be formatted as 'DAT $00...' or 'DAT \"Hello, World!\"'. Line {}.", line_num + 1);
							assembly_successful = false;
							continue;
						}
					} else {
						error!("Syntax error, unnecessary token. Data must be formatted as 'DAT $00...' or 'DAT \"Hello, World!\"'. Line {}.", line_num + 1);
						assembly_successful = false;
						continue;
					}
				}
				_ => {
					error!("Syntax error, unrecognized token {}. Line {}.", tokens[0], line_num + 1);
					assembly_successful = false;
					continue;
				}
			}
			
			// Update the address
			addr += instruction_len as u16;
			lines.push((tokens, line_num));//only push the line if it has actual code in it
		}
		if let Some(end) = end_addr {
			if addr > end {
				error!("Binary too large. An end address was specified and the length of the binary would exceed it. Consider shortening your code or increasing the end address.");
				assembly_successful = false;
			}
		}
		
		// second pass for code generation and mapping labels to addresses
		addr = 0;
		let mut hex_dump: Vec<u8> = Vec::new();
		for (tokens, line_num) in lines.iter() {
			// Parse instruction
			if !match tokens[0].as_str() {
				"lda" => match_operand!(tokens[1].as_str(), hex_dump, 0xA9, 0xAD, labels, addr),
				"sta" => match_operand!(tokens[1].as_str(), hex_dump, 0x8D, labels, addr),
				"adc" => match_operand!(tokens[1].as_str(), hex_dump, 0x6D, labels, addr),
				"ldx" => match_operand!(tokens[1].as_str(), hex_dump, 0xA2, 0xAE, labels, addr),
				"ldy" => match_operand!(tokens[1].as_str(), hex_dump, 0xA0, 0xAC, labels, addr),
				"cpx" => match_operand!(tokens[1].as_str(), hex_dump, 0xEC, labels, addr),
				"inc" => match_operand!(tokens[1].as_str(), hex_dump, 0xEE, labels, addr),
				"bne" => {
					if Regex::new(r"^\$[0-9a-fA-F]{2}$").unwrap().is_match(tokens[1].as_str()) {
						// relative address
						if let Some(operand) = parse_hex!(tokens[1], "$", u8) {
							hex_dump.push(0xD0);
							hex_dump.push(operand);
							addr += 2;
							true
						} else {
							false
						}
					} else if Regex::new(LABEL).unwrap().is_match(tokens[1].as_str()) {
						if let Some((operand, _)) = labels.get(tokens[1].as_str()) {
							addr += 2;
							let mut diff: u16 = operand.wrapping_sub(addr);
							if diff > 0x00FF && diff < 0xFF00 {
								error!("Branch error. Target address given by label {} is too far for relative addressing. Consider chain branching instead. Line {}.", tokens[1], line_num + 1);
								assembly_successful = false;
								false
							} else {
								if let Some(end) = end_addr {
									//wrap around end
									if *operand < addr {
										diff = end - addr + operand;
									}
								}
								//two's comp
								hex_dump.push(0xD0);
								hex_dump.push(diff as u8);
								true
							}
						} else {
							false
						}
					} else {
						false
					}
				},
				"dat" => {
					if Regex::new(r"^\$([0-9a-fA-F]{2})+$").unwrap().is_match(tokens[1].as_str()) {
						// raw bytes
						match tokens[1].strip_prefix("$")
							.unwrap()
							.as_bytes()
							.chunks(2)
							.map(|chunk| {
								let hex_pair: &str = std::str::from_utf8(chunk).map_err(|_| "Invalid UTF-8 in hex string")?;
								u8::from_str_radix(hex_pair, 16).map_err(|_| format!("Invalid hex byte: {}", hex_pair))
							})
							.collect()
						{
							Ok(mut vec) => {
								hex_dump.append(&mut vec);
								addr += vec.len() as u16;
								true
							},
							Err(err) => {
								error!("{}", err);
								false
							}
						}
					} else if Regex::new(QUOTES).unwrap().is_match(tokens[1].as_str()) {
						// string
						let mut bytes: Vec<u8> = parse_string!(tokens[1]).as_bytes().to_vec();
						bytes.push(0x00);//null-terminator
						hex_dump.append(&mut bytes);
						addr += bytes.len() as u16;
						true
					} else {
						false
					}
				},
				"sys" => {
					//num == 1
					//LDX #$01
					//LDY label or absolute address
					//SYS
					//instruction_len = 6
					
					//num == 2
					//LDX #$02
					//LDY relative address
					//SYS
					//instruction_len = 5
					
					//num == 3
					//LDX #$03
					//SYS label or absolute address
					//instruction_len = 5
					if tokens.len() == 1 {
						hex_dump.push(0xFF);
						true
					} else {
						let num: u8 = u8::from_str(tokens[2].as_str()).unwrap();
						let operand: Option<u16> = if Regex::new(ADDR).unwrap().is_match(tokens[1].as_str()) {
							// absolute address
							parse_hex!(tokens[1], "$", u16)
						} else if Regex::new(LABEL).unwrap().is_match(tokens[1].as_str()) {
							// label
							if let Some((operand, _)) = labels.get(tokens[1].as_str()) {
								Some(*operand)
							} else {
								None
							}
						} else {
							None
						};
						if let Some(operand) = operand {
							match num {
								1 => {
									let operand: [u8; 2] = operand.to_le_bytes();
									hex_dump.append(&mut vec![0xA2, 0x01, 0xAC, operand[0], operand[1], 0xFF]);
									addr += 6;
									true
								}
								2 => {
									addr += 5;
									let rel_addr: Option<u8> = if end_addr.is_some() {
										Some(operand as u8)
									} else {
										let diff: u16 = operand.wrapping_sub(addr);
										if diff > 0x00FF && diff < 0xFF00 {
											error!("System call error. Target address given by {} is too far for relative addressing. Line {}.", tokens[1], line_num + 1);
											assembly_successful = false;
											None
										} else {
											Some(diff as u8)
										}
									};
									if let Some(a) = rel_addr {
										hex_dump.append(&mut vec![0xA2, 0x02, 0xA0, a, 0xFF]);
										true
									} else {
										false
									}
								}
								3 => {
									let operand: [u8; 2] = operand.to_le_bytes();
									hex_dump.append(&mut vec![0xA2, 0x03, 0xFF, operand[0], operand[1]]);
									addr += 5;
									true
								}
								_ => { false }
							}
						} else {
							false
						}
					}
				},
				"txa" => {hex_dump.push(0x8A); addr += 1; true},
				"tya" => {hex_dump.push(0x98); addr += 1; true},
				"tax" => {hex_dump.push(0xAA); addr += 1; true},
				"tay" => {hex_dump.push(0xA8); addr += 1; true},
				"nop" => {hex_dump.push(0xEA); addr += 1; true},
				"brk" => {hex_dump.push(0x00); addr += 1; true},
				_ => false
			} /*if match statement returned false*/ {
				assembly_successful = false;
				continue;
			}
		}
		
		if !assembly_successful {
			log!("Assembly failed due to previous errors.");
			process::exit(1);
		}
		
		//create output content
		let prefix: bool = matches.get_flag("prefix");
		let separator_string: &String = matches.get_one::<String>("separator").unwrap();
		output_content = hex_dump.iter()
			.map(|byte| format!("{}{:02X}", if prefix {"0x"} else {""}, byte)) // Format each byte as two uppercase hex digits
			.collect::<Vec<String>>()
			.join(separator_string);
	} else {
		error!("Failed to open input file.");
		process::exit(1);
	};
	
	// Write to the output file
	if let Ok(mut output_file) = File::create(output_path) {
		if output_file.write_all(output_content.as_bytes()).is_err() {
			error!("Failed to write to output file.");
			process::exit(1);
		}
	} else {
		error!("Failed to open output file.");
		process::exit(1);
	};
	log!("Assembled successfully!");
}