(define (main)
    (println "Author: Matthew Propp matthewpropp@westminster.net")
)

; Exercise 1.2
; Expected result = -0.2466667 (code returns correct result)
(inspect (/ (+ 5 4 (- 2 (- 3 (+ 6 (/ 4.0 5)))))
    (* 3 (- 6 2) (- 2 7))))

; Exercise 1.3
define (squareOfLargerPair a b c) 
    (if (> a b) (+ (* a a) (* (max b c) (max b c)))
        (+ (* b b) (* (max a c) (max a c)))))
(println (squareOfLargerPair 10 5 9))
;above print 181 which is correct

; Excercise 1.4
(define (a-plus-abs-b a b)
  ((if (> b 0) + -) a b))
; The procdure above works by either substuting either + or - for the operator
; for the operation on a and b depending on the result of the if statement.
; This makes it so if b is positive, the + operator is usee, but if b is negative
; or 0, the minus operator is used.