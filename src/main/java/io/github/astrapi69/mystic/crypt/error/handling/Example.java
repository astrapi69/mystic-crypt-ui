package io.github.astrapi69.mystic.crypt.error.handling;

public class Example {


  public void divideByZeroWithCatch(){
    try{
      int a = 5/0;
    }
    catch (ArithmeticException e){
      System.out.println("Can not divide by zero");
    }
  }

  public void divideByZeroWithNoCatch(){
    int b = 5/0;
  }

  public static void main (String [] args){
    Example e = new Example();
    System.out.println("***** Calling method with catch block *****");
    e.divideByZeroWithCatch();
    System.out.println("***** Calling method without catch block *****");
    e.divideByZeroWithNoCatch();
  }
}
