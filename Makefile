
BlockJ.class: BlockJ.java gson-2.8.2.jar
	javac -cp "gson-2.8.2.jar" BlockJ.java

BlockInputG.class: BlockInputG.java gson-2.8.2.jar
	javac -cp "gson-2.8.2.jar" BlockInputG.java

.PHONY: clean
clean: 
	rm -f *.class
