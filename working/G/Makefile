
run: Key.class
	java -cp ".:gson-2.8.2.jar" Key

Key.class: Key.java
	javac -cp "gson-2.8.2.jar" Key.java

.PHONY: clean
clean:
	rm -f *.class *.json
