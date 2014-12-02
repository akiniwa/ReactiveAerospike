package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.Bin
import AerospikeValue.AerospikeValueConverter

case class AerospikeBin[T <: Any]
				(name: String, value: AerospikeValue[T],
				converter: AerospikeValueConverter[T]) {

  val inner = new Bin(name, value.inner)

  def toRecordValue = (name -> value.inner.getObject)

}

object AerospikeBin {
  
  def apply[T <: Any](name: String, value: T)
  	(implicit converter: AerospikeValueConverter[T]): AerospikeBin[T] = {
    AerospikeBin(name, converter.toAsV(value), converter)
  }

  def apply[T <: Any](tuple: (String, T))
  	(implicit converter: AerospikeValueConverter[T]): AerospikeBin[T] =
		  AerospikeBin(tuple._1, converter.toAsV(tuple._2), converter)
  
}