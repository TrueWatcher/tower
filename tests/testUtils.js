"use strict";
// JavaScript utilities for browser-testing of single-page applications
// TrueWatcher 2017-2018
// v.1.2.0 oct 2017 added try-catch around eval, assertContains, assertNotContains
//

function TestHelper() {
  // It's a SINGLETON
  if (typeof TestHelper.instance === 'object') return TestHelper.instance;

  //var _this=this;
  var _outTarget="console";
  var _outElement="";
  var _numOfTests=0;

  this.toPage=function(){
    _outTarget="page";
    _outElement=document.createElement("pre");
    document.body.appendChild(_outElement);
    _outElement.style="font-size: 1.4em";
    _numOfTests=0;
  };

  this.addToPage=function(str) {
    var h=_outElement.innerHTML;
    _outElement.innerHTML=h+str;
  };

  this.toConsole=function() {
    _outTarget="console";
    _numOfTests=0;
  };

  this.checkToPage=function() { return (_outTarget=="page"); };

  this.incCount=function() { _numOfTests++ };
  this.getCount=function() { return(_numOfTests) };

  TestHelper.instance=this;
}

function print(str) {
  var t=new TestHelper();
  if (str==">page") {
    t.toPage();
    return;
  }
  if (str==">console") {
    t.toConsole();
    return;
  }
  if (t.checkToPage()) {
    t.addToPage(str);
  }
  else console.log(str);
}

function println(str) {
  if (typeof str == "undefined") str="";
  print(str+"\n");
}

function printErr(err) {
  var t=new TestHelper();
  var out="",f="",i;
  out+="Terminated in/after "+t.getCount()+"th test on: "+err;
  if (err.lineNumber) {
    var fn=err.fileName;
    if ( i=fn.lastIndexOf("/") ) {
      f=fn.substr(i+1,fn.length-i-1);
    }
    out+=" ("+f+":"+err.lineNumber+")";
  }
  println(out);
}

function TestError(message) {
  this.name = 'TestError';
  this.message = message;
  this.stack = (new Error()).stack;
}
TestError.prototype = new Error();

function assertTrue(statement,message,messageOK) {
  var t=new TestHelper(),out="";
  t.incCount();
  if (  !statement) {
    println("Failure:"+message);
    throw new TestError (""+t.getCount());
  }
  else {
    out+="Passed "+t.getCount();
    if(messageOK) out+=": "+messageOK;
    println(out);
  }
}

function assertEqualsPrim(expected,found,message,messageOK) {
  var t=new TestHelper(),out="";
  t.incCount();
  if( !(expected==found) ) {
    println("Failure: '"+found+"' does not equal to expected '"+expected+"' \n"+message+"\n");
    throw new TestError (""+t.getCount());
  }
  else {
    out+="Passed "+t.getCount();
    if (messageOK) out+=": "+messageOK;
    println(out);
  }
}

function assertEqualsVect(expected,found,message,messageOK) {
  if ( ! (expected instanceof Array) ) throw new TestError ("assertEqualsVect:1st argument is not Array");
  if ( ! (found instanceof Array) ) throw new TestError ("assertEqualsVect:2nd argument is not Array");
  assertEqualsPrim(expected.join(),found.join(),message,messageOK);
}

function assertContains(expected,haystack,message,messageOK) {
  var t=new TestHelper(),out="";
  t.incCount();
  if ( ! haystack || typeof haystack == "undefined") {
    println("Failure: haystack is empty ("+typeof haystack+")\n"+message+"\n");
    throw new TestError (""+t.getCount());
  }
  if( haystack.indexOf(expected) < 0 ) {
    println("Failure: '"+haystack+"' does not contain '"+expected+"' \n"+message+"\n");
    throw new TestError (""+t.getCount());
  }
  else {
    out+="Passed "+t.getCount();
    if (messageOK) out+=": "+messageOK;
    println(out);
  }
}

function assertNotContains(expected,haystack,message,messageOK) {
  var t=new TestHelper(),out="";
  t.incCount();
  if( haystack.indexOf(expected) >= 0 ) {
    println("Failure: '"+haystack+"' actually contains '"+expected+"' \n"+message+"\n");
    throw new TestError (""+t.getCount());
  }
  else {
    out+="Passed "+t.getCount();
    if (messageOK) out+=": "+messageOK;
    println(out);
  }
}

function translateArray(arr) {
  var rn=new Seq2d();
  var rc,r,c,s,i,res=[];
  for (i=0; i<DIM; i++) {  res.push( (new Array(DIM)).fill(0) ) }
  while ( rc=rn.go() ) {
    r=rc[0];
    c=rc[1];
    if ( typeof arr[r] == "undefined" || arr[r][c]==0 ) s="e";
    else s="s";
    res[r][c] = s;
  }
  return res;
}

  /**
   * Executes commands one by one with pause.
   * Evals one and spawns a process of calling itself.
   * @param {object CommandIterator} commandIterator
   * @return void
   */
  function commandsRun(commandIterator) {
    var now=commandIterator.go();
    //alert("about to execute:"+now);
    try {
      eval(now);
    }
    catch (e) {
      if (e instanceof TestError) {
        console.log(e.message);
        throw new Error(e.message);
      }
      console.log("Line:"+now);
      console.log("Error:"+e.message);
      if (! commandIterator.isRelaxed()) throw new Error(e.message);
      commandIterator.clearRelaxed();
    }
    if ( ! commandIterator.isStopped()) {
      setTimeout(
        function() { commandsRun(commandIterator); }
      ,500);
    }
  }
  
  /**
   * 
   * @constructor
   */
  function CommandIterator(lines,maxRounds) {
    if (!(lines instanceof Array)) throw new TestError ("Non-array argument "+(typeof lines));
    var index=0, l=lines.length, rounds=0;
    if (typeof maxRounds == "undefined") maxRounds=300;
    var stopped=false,
        looping=false,
        relaxed=false;
    
    /**
     * 
     * @return string
     */
    this.go=function() {
      var r="";
      if (stopped) {
        return("");
      }
      if (typeof lines[index] == "undefined" ) {
        this.stop();
        return("");
      }
      r=lines[index];
      rounds+=1;
      if (rounds > maxRounds) {
        //console.log("maxRounds exceeded");
        throw new TestError ("Limit of "+maxRounds+" commands exceeded");
      }  
      if ( ! looping) {
        index+=1;
      }
      if (index >= l) {
        this.stop();
      }
      return r;
    };
    
    this.loop=function() { looping=true; };
    
    this.noLoop=function() { looping=false; };
    
    this.stop=function() {
      console.log("CommandIterator finished after "+rounds+" rounds");
      stopped=1;
    };
    
    this.isStopped=function() { return stopped; };
    
    this.inc=function() {
      if (looping) index+=1;
      if ( index>=l ) { this.stop(); }
    };
    
    this.isRelaxed=function() { return relaxed; }
    this.setRelaxed=function() { relaxed=true; }
    this.clearRelaxed=function() { relaxed=false; }
  }
  
  function cl(id) {
    document.getElementById(id).click();
  }
