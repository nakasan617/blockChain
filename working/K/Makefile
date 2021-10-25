
0: BlockInput.class
	java -cp ".:gson-2.8.2.jar" BlockInput
1: BlockInput.class
	java -cp ".:gson-2.8.2.jar" BlockInput 1
2: BlockInput.class
	java -cp ".:gson-2.8.2.jar" BlockInput 2

BlockInput.class: BlockInput.java
	javac -cp "gson-2.8.2.jar" BlockInput.java

.PHONY: clean
clean: 
	rm -f *.class
