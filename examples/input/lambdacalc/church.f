0 = \x . \y . y
s = \x . \y . \z . y (x y z)

1 = s 0
2 = s 0

+ = \n1 . \n2 . \f . \x . n2 f (n1 f x)
* = \n1 . \n2 . \f . n1 (n2 f)

-----------------

+ 1 2
