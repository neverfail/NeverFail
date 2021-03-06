### Bytecode manipulation detail

NeverFail patch two functions  
This file intend to explain what the patch does and how.

We need to patch two function because we haven't enough space (bytes) to write on just one.

Basically we rewrite `Score()` function that return the score integer as the new following equivalent pseudo-code:
```
// condition to apply cheat, quiz is failed
if(this.m_nScore < this.PassScore) {
   // compute a random mark
   var goal = ((Math.random() * 40) + 55) * this.MaxScore / 100;

   // add questions maximum points until we reach random mark
   var i = this.m_nScore = 0;
   do {
       // add question maximum points
       this.m_nScore += this.m_arrInteractions[i++].MaxScore;
       // stop when goal is reached
       if(this.m_nScore > goal) break;
   } while(i < this.m_arrInteractions.length);
}

// normal behavior of function
return this.m_nScore;
```
But all this code does not fill in `Score()` body, so wa use the variable `m_nScore` as a switch for cheating  
when `m_nScore` is negative, we deduce it's a call from `Score()` to continue cheat with `Passed()`

 * Function `Score()`
```
// condition to apply cheat, quiz is failed
if(this.m_nScore < this.PassScore) {
   // compute a random mark (this time it's negated)
   var this.m_nScore = - (((Math.random() * 40) + 55) * this.MaxScore / 100);

   // delegate work to Passed()
   Passed();
}
```

 * Function `Passed()`
 ```
// switch with m_nScore, negative is a call from Score()
if(this.m_nScore < 0) {
   // set our goal by negating m_nScore
   var goal = -this.m_nScore;

   // next is the same process
   // add questions maximum points until we reach random mark
   var i = this.m_nScore = 0;
   do {
       // add question maximum points
       this.m_nScore += this.m_arrInteractions[i++].MaxScore;
       // stop when goal is reached
       if(this.m_nScore > goal) break;
   } while(i < this.m_arrInteractions.length);
}

// we alwais passed quiz
return true;
```

The previous codes have been translated into these bytecode instructions:
 
 * Function `Score()`
```
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; public function get Passed() : Boolean ;;;
_as3_getlocal <0>  							//d0
_as3_pushscope  							//30

; if(this.m_nScore < 0)
_as3_getlocal <0>  							//d0
_as3_getproperty m_nScore 					//66 %m_nScore%
_as3_pushbyte 0 							//24 00
_as3_ifnlt offset: 67 						//0c 43 00 00

; var goal = - this.m_nScore;
_as3_getlocal <0>  							//d0
_as3_getproperty m_nScore 					//66 %m_nScore%
_as3_negate  								//90
_as3_coerce_a  								//82
_as3_setlocal <1>  							//d5

; var i = this.m_nScore = 0;
_as3_getlocal <0>  							//d0
_as3_pushbyte 0 							//24 00
_as3_setproperty m_nScore 					//61 %m_nScore%

_as3_pushbyte 0 							//24 00
_as3_setlocal <2>  							//d6


; (loop start)
_as3_jump offset: 33 + 6					//10 23 00 00

; this.m_nScore += this.m_arrInteractions[i++].MaxScore;
#-43 _as3_label  							//09
_as3_getlocal <0>  							//d0
_as3_getlocal <0>  							//d0
_as3_getproperty m_nScore 					//66 %m_nScore%
_as3_getlocal <0>  							//d0
_as3_getproperty m_arrInteractions 			//66 %m_arrInteractions%
_as3_getlocal <2>  							//d2
_as3_convert_d  							//75
_as3_dup  									//2a
_as3_increment  							//91
_as3_coerce_a  								//82
_as3_setlocal <2>  							//d6
_as3_getproperty {} 						//66 %{}%
_as3_getproperty MaxScore 					//66 %MaxScore%
_as3_add  									//a0
_as3_setproperty m_nScore 					//61 %m_nScore%

; if(this.m_nScore > goal) break;
_as3_getlocal <0>  							//d0
_as3_getproperty m_nScore 					//66 %m_nScore%
_as3_getlocal <1>  							//d1
_as3_ifgt offset: 10 + 2					//17 0c 00 00

; while(i < this.m_arrInteractions.length)
#33 _as3_getlocal <2>  						//d2
_as3_getlocal <0>  							//d0
_as3_getproperty m_arrInteractions 			//66 %m_arrInteractions%
_as3_getproperty length 					//66 %length%
_as3_iflt offset: -43 - 8					//15 d1 ff ff

; return true;
_as3_pushtrue  								//26
_as3_returnvalue 							//48
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
```

 * Function `Passed()`
```
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; public function get Score() : Number   ;;;
_as3_getlocal <0>  							//d0
_as3_pushscope  							//30

; if(this.m_nScore < this.PassScore)
_as3_getlocal <0>  							//d0
_as3_getproperty m_nScore 					//66 %m_nScore%
_as3_getlocal <0>  							//d0
_as3_getproperty PassScore 					//66 %PassScore%
_as3_ifnlt offset: 26 + 5	1f				//0c 1f 00 00

; this.m_nScore = - (((Math.random() * 40) + 55) * this.MaxScore / 100);
_as3_getlocal <0>  							//d0
_as3_getlex Math 							//60 %Math%
_as3_callproperty random(param count:0) 	//46 %random% 00
_as3_pushbyte 40 							//24 28
_as3_multiply  								//a2
_as3_pushbyte 55 							//24 37
_as3_add  									//a0
_as3_getlocal <0>  							//d0
_as3_getproperty MaxScore 					//66 %MaxScore%
_as3_multiply  								//a2
_as3_pushbyte 100 							//24 64
_as3_divide  								//a3
_as3_negate  								//90
_as3_setproperty m_nScore 					//61 %m_nScore%

; this.Passed
_as3_getlocal <0>  							//d0
_as3_getproperty Passed 					//66 %Passed%
_as3_setlocal <1>							//d5

; return this.m_nScore;
_as3_getlocal <0>  							//d0
_as3_getproperty m_nScore 					//66 %m_nScore%
_as3_returnvalue  							//48
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
```
