# Path language

## Table of Contents:
* [Main rules](#main-rules)
* [Functions](#functions)
* [More complex examples](#more-complex-examples)

## Main rules:

1. Expression starts from open parentheses.
2. There are built-in and external functions.
3. External functions have namespace ext (e.g. ext/list-cars)
4. Expression can accept several arguments and any argument can be a collection.
5. Expression can accept another expression as argument and it will be evaluated for getting it's value and using it as argument to upper expression.
6. There are context values:
    * $ - root element (required item);
    * % - current element in expressions that are used inside filter, map, etc;
7. Dynamic context must be passed and it must contain $ (root element), used external functions. It can contain other elements that were used inside expressions.
    ```clojure
    {$ car
     z {:x 1 :y 2}
     ext/list-cars (fn [start end]
                     ...)
     ext/foo (fn [] 42)}
    ```

## Functions:

### `keyword (e.g. :car/color)` - ('keyword' arg1 & args)
Accepts one or more arguments which can be an atomic value, a collection of datomic
entities or maps. Applies to each argument clojure function `get`. Returns an atomic
value, a collection of datomic entities, maps or nil values. Throws error if atomic
value isn't datomic entity or map.
```clojure
(:foo {:foo 1} ({:foo 2} {:bar 1}) ()) => (1 2 nil)
```
 
### `=` - (arg1 arg2 & args)
Accepts two or more arguments which can be an atomic value or a collection of atomic
values. Throws error if collection contains more than one element or zero elements.
Converts collections to atomic values (extracts it's value). Throws error if atomic
values have different types. Returns true if all atomic values of arguments are equal.
```clojure
(= "red" ("green") "blue") => false
(= "red" ("red") "red") => true
(= "red" nil) => false
(= "red" ("red" "green")) => throws error
(= "red" (1) {:x 1}) => throws error
(= "red" ((1))) => throws error
(= "red") => throws error
(= "red" ()) => throws error
```

### `not=` - (arg1 arg2 & args)
Accepts two or more arguments which can be an atomic value or a collection of atomic
values. Throws error if collection contains more than one element or zero elements.
Converts collections to atomic values (extracts it's value). Throws error if atomic
values have different types. Returns true if any atomic values differ from antoher.
```clojure
(not= "red" ("green") "blue") => true
(not= "red" ("red") "red") => false
(not= "red" nil) => true
(not= "red" ("red" "green")) => throws error
(not= "red" (1) {:x 1}) => throws error
(not= "red" ((1))) => throws error
(not= "red") => throws error
(not= "red" ()) => throws error
```

### `>` - (arg1 arg2 & args)
Accepts two or more arguments which can be an atomic value or a collection of atomic
values. Throws error if collection contains more than one element or zero elements.
Converts collections to atomic values (extracts it's value). Throws error if atomic
values have different types. Supports comparison of numbers and dates. Returns true
if each successive element strictly more than previous otherwise returns false.
```clojure
(> 1 2) => false
(> 2 1 -1) => true
(> 2 (1) (-1)) => true
(> 2 (1 2)) => throws error
(> 2 "1") => throws error
```

### `<` - (arg1 arg2 & args)
Accepts two or more arguments which can be an atomic value or a collection of atomic
values. Throws error if collection contains more than one element or zero elements.
Converts collections to atomic values (extracts it's value). Throws error if atomic
values have different types. Supports comparison of numbers and dates. Returns true
if each successive element strictly less than previous otherwise returns false.
```clojure
(< 1 2) => true
(< 1 2 (3)) => true
(< 2 1 -1) => false
(< 2 (1) (-1)) => false
(< 2 (1 2)) => throws error
(< 2 "1") => throws error
```

### `>=` - (arg1 arg2 & args)
Accepts two or more arguments which can be an atomic value or a collection of atomic
values. Throws error if collection contains more than one element or zero elements.
Converts collections to atomic values (extracts it's value). Throws error if atomic
values have different types. Supports comparison of numbers and dates. Returns true
if each successive element more or equal than previous otherwise returns false.
```clojure
(>= 1 2) => false
(>= 2 1 -1) => true
(>= 2 2 -1) => true
(>= 2 2 2) => true
(>= 2 (1) (-1)) => true
(>= 2 (1 2)) => throws error
(>= 2 "1") => throws error
```

### `<=` - (arg1 arg2 & args)
Accepts two or more arguments which can be an atomic value or a collection of atomic
values. Throws error if collection contains more than one element or zero elements.
Converts collections to atomic values (extracts it's value). Throws error if atomic
values have different types. Supports comparison of numbers and dates. Returns true
if each successive element less or equal than previous otherwise returns false.
```clojure
(<= 1 2) => true
(<= 1 2 (3)) => true
(<= 2 2 (3)) => true
(<= 2 2 (2)) => true
(<= 2 1 -1) => false
(<= 2 (1) (-1)) => false
(<= 2 (1 2)) => throws error
(<= 2 "1") => throws error
```

### `+` - (arg1 arg2 & args)
Accepts two or more arguments which can be an atomic value or a collection of atomic
values. Throws error if collection contains more than one element or zero elements.
Converts collections to atomic values (extracts it's value). Throws error if atomic
values have different types. Supports numbers, dates and strings. Returns sum of all
elements.
```clojure
(+ 1 2) => 3
(+ 1 2 (3)) => 6
(+ (date 2010 10 10) (hours 8) (minutes 10)) => #inst "2010-10-10T8:10:00"
(+ ("x") "y" ("z")) => "xyz"
(+ {:x 1} {:y 2}) => throws error
(+ 2 "1") => throws error
```

### `*` - (arg1 arg2 & args)
Accepts two or more arguments which can be an atomic value or a collection of atomic
values. Throws error if collection contains more than one element or zero elements.
Converts collections to atomic values (extracts it's value). Throws error if atomic
values have different types. Supports numbers. Returns product of all elements.
```clojure
(* 1 2) => 2
(* 1 2 (3)) => 6
(+ ("x") "y" ("z")) => throws error
(+ {:x 1} {:y 2}) => throws error
(+ 2 "1") => throws error
```

### `-` - (arg1 arg2 & args)
Accepts two or more arguments which can be an atomic value or a collection of atomic
values. Throws error if collection contains more than one element or zero elements.
Converts collections to atomic values (extracts it's value). Throws error if atomic
values have different types. Supports numbers and dates. Returns difference between
first element and sum of all successive elements.
```clojure
(- 1 2) => -1
(- 1 2 (3)) => -4
(- (datetime 2010 10 10 8 10) (hours 8) (minutes 10)) => #inst "2010-10-10T00:00:00"
(- {:x 1} {:y 2}) => throws error
(- 2 "1") => throws error
```

### `/` - (arg1 arg2 & args)
Accepts two or more arguments which can be an atomic value or a collection of atomic
values. Throws error if collection contains more than one element or zero elements.
Converts collections to atomic values (extracts it's value). Throws error if atomic
values have different types. Supports numbers and dates. Returns quotient between
first element and product of all successive elements.
```clojure
(/ 4 2) => 2
(/ 30 2 5) => 3
(/ {:x 1} {:y 2}) => throws error
(/ 2 "1") => throws error
```

### `sum` - (arg1 arg2 & args)
Accepts two or more arguments which can be an atomic value or a collection of atomic
values. Throws error if not all atomic values have same types. Supports numbers. Returns
sum of all elements of all collection.
```clojure
(sum 1 2) => 3
(sum 1 2 (3)) => 6
(sum (1 2 3)) => 6
(sum (1 2 3) 1) => 7
(+ {:x 1} {:y 2}) => throws error
(+ 2 "1") => throws error
```

### `product` - (arg1 arg2 & args)
Accepts two or more arguments which can be an atomic value or a collection of atomic
values. Throws error if not all atomic values have same types. Supports numbers. Returns
product of all elements of all collection.
```clojure
(* 1 2) => 2
(* 1 2 (3)) => 6
(* (1 2 3)) => 6
(* (1 2 3) 4) => 24
(* ("x") "y" ("z")) => throws error
(* {:x 1} {:y 2}) => throws error
(* 2 "1") => throws error
```

### `list` - (& args)
Accepts any number of arguments which must be an atomic value. Returns collection consists
of passed arguments.
```clojure
(list 1 2) => (1 2)
(list 1 2 (3)) => (1 2 (3))
(list 1 "2" {:x 1}) => (1 "2" {:x 1})
(list) => ()
```

### `filter` - (pred arg & args)
Accepts two or more arguments. First argument must be an expression which works on atomic
value and returns atomic value or collection with one element. If this expression returns
false, empty collection or nil they treat as logical false other values treat as logical
true. Second and other arguments can be an atomic value or collection of atomic values. Function
processes each atomic value, passes it into pred expression and if pred returns logical true
then it will be added into resulting collection. Inside pred symbol % may be used in order to
point on currently proccessing element.
```clojure
(filter (= % 1) (1 2 3) (4 5 6) 1) => (1 1)
(filter % (1 2 3) (4 5 6 nil false) 7 ()) => (1 2 3 4 5 6 7)
(filter (:x %) ({:x 1} {:x 2})) => (1 2)
```

### `map` - (fn args & args)
Accepts two or more arguments. First argument must be an expression which works on atomic
value and returns transformed value. Function processes each atomic value, passes it into
fn expression and adds it into resulting collection. Inside fn symbol % may be used in order
to point on currently proccessing element.
```clojure
(map (+ % 1) (1 2 3) 4 (5)) => (2 3 4 5 6)
(map (:x %) {:x 1}) => (1)
```

### `if` - (test then else)
Accepts exactly three arguments. Argument test is an expression which must return logical true or
false (empty collection or nil are treated as logical false other values are treated as logical true).
Arguments then and else can be any valid expression. If test returns logical true then then expression
will be evaluated otherwise else expression will be evaluated.
```clojure
(if (= 1 2)
(+ 1 2)
(- 2 1)) => 1
(if (= 1 1)
(+ 1 2)
(- 2 1)) => 3
```

### `select-keys` - (fn args & args)
Accepts two and more arguments. First argument must be an expression which works on atomic
value and returns collection of keyword which must be gathered from processing element. Function
works with maps and datomic entities.
```clojure
(select-keys (list :x :y)
            ({:x 1 :z 2} {:y 1 :w 2})
        {:x 3 :y 4}) => ({:x 1} {:y 1} {:x 3 :y 4})
(select-keys (if (= (:x %) 1)
            (list :y)
    (list :z))
        ({:x 1 :y 2}
        {:x 2 :y 3}
        {:x 3 :z 4})) => ({:y 2} {} {:z 4})
```

### `count` - (args & args)
Accepts one or more arguments. Each argument can be an atomic value or collection. Returns sum
of all collection lengths. Atomic value is assumed to be a collection with one eleemtn.
```clojure
(count (1 2 3)) => 3
(count 1 2 3) => 3
(count (1) (2 3) 4) => 4
```

### `now` - ()
Accepts now arguments. Returns now datetime.
```clojure
(now) => #inst "2021-01-01T20:00"
```

### `years`, `months`, `days`, `hours`, `minutes` - (n)
Accepts one argument which must be a number. Returns structure that can be used for adding or
subtracting to date.
```clojure
(years 1) => <not graphical representation>
(months 7) => <not graphical representation>
```

### `year-start`, `month-start`, `day-start` - (d)
Accepts one argument which must be a datetime. Returns start of the year, month or day respectively.
```clojure
(year-start (date 2010 10 10)) => #inst "2010-01-01T00:00"
(month-start (date 2010 10 10)) => #inst "2010-10-01T00:00"
(day-start (datetime 2010 10 10 10 00)) => #inst "2010-10-10T00:00"
```

### `date` - (year month day)
Accepts three arguments which must be a numbers. Returns start of the date.
```clojure
(date 2010 10 10) => #inst "2010-10-10"
```

### `datetime` - (year month day hour minute)
Accepts five arguments which must be a number. Returns date with particular time.
```clojure
(datetime 2010 10 10 17 00) => #inst "2010-10-10T17:00:00"
```

### `at-zone` (datetime timezone)
Accepts two arguments. First argument must be a datetime and second argument must be a timezone.
Converts datetime to timezone datetime.
```clojure
(at-zone (datetime 2010 10 10 3 0) <Moscow timezone>) => #inst "2010-10-10T06:00"
```


## More complex examples:

Check that car has black color. Search inside :car-state with feature :color
and extracts value-code from it.
```clojure
(evaluate
 (=
  (:car-state/value-code
   (filter (= (:feature/code (:car-state/feature %)) :color)
           (:car/state $)))
  “black”)
 {'$ car})
```

Searches car-states for feature with id 70 or 90, extracts their long-value,
sum their values and compare with 1000.
```clojure
(evaluate
 (>
  (sum
   (:car-state/long-value
    (filter
     (or (= (:db/id (:car-state/feature %)) 70)
         (= (:db/id (:car-state/feature %)) 90))
     (:car/state $))))
  1000)
 {'$ car})
```

If car has statement for feature :feature-00001 then return it's :car/name
```clojure
(evaluate
 (if (filter
      (= (:feature/code
          (:car-state/feature %))
         :feature-00001)
      (:car/state $))
   (select-keys (list :car/name) $)
   ())
 {'$ car})
```
```clojure
(evaluate
 (select-keys
  (list :car/name)
  (filter
   (= (:feature/code
       (:car-state/feature %))
      :feature-00001)
   (:car/state $)))
 {'$ car})
```

Checkes if cars count plus one for last month more then ten.
```clojure
(evaluate
 (>
  (+
   (count
    (ext/list-cars (now) (- (now) (months 1))))
   1)
  10)
 {'ext/list-cars list-cars})
```