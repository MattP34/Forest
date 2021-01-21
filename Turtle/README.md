#Welcome to the Turtle Programming Language!

This language was created by Matthew Propp as part of the Honors Programming
Languages Curriculum at Westminster in Spring 2021.

##Variables
Turtle is a strongly typed language

Turtle supports the following data types
```
boolean
byte
short
int
long
float
double
char
```

Variables are declared using the `set` operator
```
int x set 1;
double y set 1.1;
```
pointers can be declared using the `ref` operator
```
int x set 1;
int y ref x;
x set 2;
```

#Functions
Functions should be commented code that can be referenced with the `goto` operator
```
/* myFunction
char str ref;
open(str,2);
str[0] set 'H';
str[1] set 'i';
echo(str,2);
goto(comeback);
/*
int comeBack = 10;
goto(1);
echo(str,2);
```
##Arrays
arrays can be created by allocating memory using the `open` operator
```
int x ref;
open(x,4);
x[1] = 0;
x[1] = 5;
x[2] = 10;
x[3] = 15;
```



