#Welcome to the Forest Programming Language!

This language was created by Matthew Propp as part of the Honors Programming
Languages Curriculum at Westminster in Spring 2021.

Forest is a programming language where the entire program is represented
by a tree data structure.

##Print
In Forest, the print command is `print(string)`
```
print("Hello World"); //prints Hello World
```

##Variables
Forest is a weakly typed language
Supported data types include
```
boolean
int
double
char
string
```
###Declaring Variables
Variables are declared from their composite values

a child of a node is created using the `->` operator

the child of a node can be accessed using the `_` operator

the `=` sets a child node equal to the vlaue of the parent node

variable values are stores by the most recent declaration but previous declaration can still be accessed through the graph
```
=(5)->x;
```
###Changing Variables
Variable values are stores by the most recent declaration but previous declaration can still be accessed through the graph
```
=(10)->x;
```
###Accessing Variables
variables can be accessed either directly by name or by parent linkage
```
print(5_x) //prints 5
print(10_x) //prints 10
```
or
```
print(x) //prints 10
```
##Functions
###Creating Functions
Functions are created using the `func` operator
Parameters are specified using `(param1, param2, ...)`
```
func avg(x,y) {
    +(x,y)->sum;
    /(sum,2)->result;
    return result;
}
```
Functions can also return multiple values
```
func sumAndAvg(x,y) {
    +(x,y)->sum;
    /(sum,2)->result;
    return sum,result;
}
```
###Calling Functions
Functions are called using funcName(param1,param2,...)
the return values are stored using the `->` operator
```
avg(x,y)->calcAverage;
sumAndAvg(x,y)->calcSum,calcAverage;
```

##Control Flow
###if, elif, else
if is used as `if(boolean)`, elif is used as `elif(boolean)` and else use simply `else`
```
>(x,y)->bool1;
<(x,y)->bool2;
if(bool1) {
    print("x is greater than y");
} elif(bool2) {
    print("y is greater than x");
} else {
    print("x and y are equal);
}
```
###while
while loops are similarly formatted to if statments but use the `while` keyword
```
=(0)->i;
<=(i,5)->bool;
while(bool) {
    +(i,1)->i;
    print(i);
    <=(i,5)->bool;
}
```
Console output
```
012345
```
##Operators
all operators in Forest act like function
###1 Parameter operators (unary)
```
! //not
= //equals (returns same value as parameter)
```
###Variable Parameter operators
these operators can take any number of parameters

if the operator normally would take a specific number of parameter (for example > taking 2)
Forest will continually apply the operator to the additional parameters given each new parameter and previous value
```
+ //addition
- //subtraction
* //multiplication
/ //division
** //power
```
Operators can also be combined into a single function using the `$` symbol

The first operator will always use the minimum parameters unless specified by following the operaotr with an integer

```
*$+(4,3,2)->x;
print(x); //prints 14

*$+(4,3,2,1)->x;
print(x); //prints 15

*2$+(4,3,2,1)->;
print(x); //prints 25
```
##Comparators
In Forest, comparators act the same as operators

If more than 2 parameters are given, the later parameters 
are compared the first parameter and `&&` are used between comparators
the comparators in forest include
```
> //greater than
< //less than
>= //greater than or equal to
<= //less than or equal to
== //equal to
|| //or
&& //and
```

```
>(3,5)->bool;
print(bool); //prints true

>(3,5,6)->bool;
print(bool); //prints true

>(3,5,2)->bool;
print(bool); //prints false
```
##Collections
Forest does not currently support arrays or lists.
Support for these will likely be added later once I better understand how to strucutre the language.

##Built-in Functions
Forest does not currently support any built-in functions other than `print`.
Later on, it will likely include functions can get values and traverse the programs tree