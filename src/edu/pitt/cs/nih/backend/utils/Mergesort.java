package edu.pitt.cs.nih.backend.utils;

public class Mergesort
{
   /**
   * The main method illustrates the use of a merge sort to sort a 
   * small array.
   * The <CODE>String</CODE> arguments (<CODE>args</CODE>) are not used 
   * in this implementation.
   **/
//   public static void main(String[ ] args)
//   {
//      final String BLANKS = "  "; // A String of two blanks
//      int i;                      // Array index
//
//      int[ ] data = { 1000, 80, 10, 50, 70, 60, 90, 20, 30, 40, 0, -1000 };
//
//      // Print the array before sorting:
//      System.out.println("Here is the entire original array:");
//      for (i = 0; i < data.length; i++)
//         System.out.print(data[i] + BLANKS);
//      System.out.println( );
//
//      // Sort the numbers, and print the result with two blanks after each number.
//      mergesort(data, 1, data.length-2);
//      System.out.println("I have sorted all but the first and last numbers.");
//      System.out.println("The numbers are now:");
//      for (i = 0; i < data.length; i++)
//         System.out.print(data[i] + BLANKS);
//      System.out.println( );
//   }
   
   
   /**
   * Sort an array of integers from smallest to largest, using a merge sort
   * algorithm.
   * @param <CODE>data</CODE>
   *   the array to be sorted
   * @param <CODE>first</CODE> 
   *   the start index for the portion of the array that will be sorted
   * @param <CODE>n</CODE>
   *   the number of elements to sort
   * <dt><b>Precondition:</b><dd>
   *   <CODE>data[first]</CODE> through <CODE>data[first+n-1]</CODE> are valid
   *   parts of the array.
   * <dt><b>Postcondition:</b><dd>
   *   If <CODE>n</CODE> is zero or negative then no work is done. Otherwise, 
   *   the elements of </CODE>data</CODE> have been rearranged so that 
   *   <CODE>data[first] &lt= data[first+1] &lt= ... &lt= data[first+n-1]</CODE>.
   * @exception ArrayIndexOutOfBoundsException
   *   Indicates that <CODE>first+n-1</CODE> is an index beyond the end of the
   *   array.
   * */
   public static void mergesort(FeatureWeight[ ] data, int first, int n, boolean ascending)
   {
      int n1; // Size of the first half of the array
      int n2; // Size of the second half of the array

      if (n > 1)
      {
         // Compute sizes of the two halves
         n1 = n / 2;
         n2 = n - n1;

         mergesort(data, first, n1, ascending);      // Sort data[first] through data[first+n1-1]
         mergesort(data, first + n1, n2, ascending); // Sort data[first+n1] to the end

         // Merge the two sorted halves.
         merge(data, first, n1, n2, ascending);
      }
   } 
  
   private static void merge(FeatureWeight[ ] data, int first, int n1, int n2, boolean ascending)
   // Precondition: data has at least n1 + n2 components starting at data[first]. The first 
   // n1 elements (from data[first] to data[first + n1 � 1] are sorted from smallest 
   // to largest, and the last n2 (from data[first + n1] to data[first + n1 + n2 - 1]) are also
   // sorted from smallest to largest. 
   // Postcondition: Starting at data[first], n1 + n2 elements of data
   // have been rearranged to be sorted from smallest to largest.
   // Note: An OutOfMemoryError can be thrown if there is insufficient
   // memory for an array of n1+n2 ints.
   {
      FeatureWeight[ ] temp = new FeatureWeight[n1+n2]; // Allocate the temporary array
      int copied  = 0; // Number of elements copied from data to temp
      int copied1 = 0; // Number copied from the first half of data
      int copied2 = 0; // Number copied from the second half of data
      int i;           // Array index to copy from temp back into data

      // Merge elements, copying from two halves of data to the temporary array.
      while ((copied1 < n1) && (copied2 < n2))
      {
    	  if(ascending){
    		  //ascending
    		  if (Math.abs(data[first + copied1].weight) < Math.abs(data[first + n1 + copied2].weight))
    			  temp[copied++] = data[first + (copied1++)];
    		  else
    			  temp[copied++] = data[first + n1 + (copied2++)];
    	  }
    	  else{
    		  //descending
    		  if (Math.abs(data[first + copied1].weight) > Math.abs(data[first + n1 + copied2].weight))
    			  temp[copied++] = data[first + (copied1++)];
    		  else
    			  temp[copied++] = data[first + n1 + (copied2++)];
    	  }
      }

      // Copy any remaining entries in the left and right subarrays.
      while (copied1 < n1)
         temp[copied++] = data[first + (copied1++)];
      while (copied2 < n2)
         temp[copied++] = data[first + n1 + (copied2++)];

      // Copy from temp back to the data array.
      for (i = 0; i < n1+n2; i++)
         data[first + i] = temp[i];
   }
}
