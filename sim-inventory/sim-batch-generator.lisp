;;
;;  A simple script just to remember  how parameters should look when
;;  we send them to the SIM factory.
;;


(defun random128bits () 
   "If you trust your implementation’s built in random number generator, this will give 128 bits of hex encoded randomness"
     (format nil "~32,'0x"   (random (expt 2 128))))


(defun milenage-params ()
  (with-output-to-string (out)
      (format out "~2%;; Rotation parameters~%")
      (format out "~% r~d = ~s" 1 64)
      (format out "~% r~d = ~s" 2 0)
      (format out "~% r~d = ~s" 3 32)
      (format out "~% r~d = ~s" 4 64)
      (format out "~% r~d = ~s" 5 96)

      (format out "~2%;; Conversion parameters~%")
      (dotimes (i 5)
	(format out "~% c~d = ~a" (+ i 1) (random128bits)))

      (format out "~2%;; Operator key~%")
      (format out "~% OP = ~a" (random128bits))))



