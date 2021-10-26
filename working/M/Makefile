
0: BlockInput.class
	java -cp ".:gson-2.8.2.jar" BlockInput
1: BlockInput.class
	java -cp ".:gson-2.8.2.jar" BlockInput 1
2: BlockInput.class
	java -cp ".:gson-2.8.2.jar" BlockInput 2

BlockInputG: BlockInputG.class
	java -cp ".:gson-2.8.2.jar" BlockInputG

BlockInput.class: BlockInput.java
	javac -cp "gson-2.8.2.jar" BlockInput.java

BlockInputG.class: BlockInputG.java
	javac -cp "gson-2.8.2.jar" BlockInputG.java

.PHONY: clean
clean: 
	rm -f *.class
