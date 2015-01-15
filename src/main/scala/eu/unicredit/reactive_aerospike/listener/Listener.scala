/* Copyright 2014 UniCredit S.p.A.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package eu.unicredit.reactive_aerospike.listener

import com.aerospike.client.{AerospikeException, Key,Record}
import com.aerospike.client.listener.{WriteListener, 
									  RecordListener, 
									  DeleteListener, 
									  ExistsListener,
									  RecordArrayListener,
									  RecordSequenceListener}
import eu.unicredit.reactive_aerospike.future.{Promise, Future, Factory}
import eu.unicredit.reactive_aerospike.data._
import AerospikeValue.AerospikeValueConverter
import scala.language.existentials
import scala.collection.immutable.Stream._

class Listener[T <: CommandResult](factory: Factory) { 
  val promise: Promise[T] =
  	if (factory == null) 
  		throw new Exception("Please explicitly define your implicit Future Factory")
  	else factory.newPromise
  val result: Future[T] = promise.future
}


class CommandResult(implicit factory: Factory) {}
case class AerospikeWriteReturn[T <: Any]
		(key: AerospikeKey[T])
		(implicit factory: Factory) 
		extends CommandResult()
case class AerospikeDeleteReturn[T <: Any]
		(key_existed: Tuple2[AerospikeKey[T], Boolean])
		(implicit factory: Factory)
		extends CommandResult 
case class AerospikeExistsReturn[T <: Any]
		(key_existed: Tuple2[AerospikeKey[T], Boolean])
		(implicit factory: Factory)
		extends CommandResult
case class AerospikeReadReturn[T <: Any](
		key_record: Tuple2[AerospikeKey[_], AerospikeRecord])
		(implicit recordReader: AerospikeRecordReader,
				  factory: Factory) 
		extends CommandResult
case class AerospikeMultipleReadReturn[T <: Any](
		key_records: Seq[(AerospikeKey[_], AerospikeRecord)])
		(implicit recordReader: Seq[AerospikeRecordReader],
				  factory: Factory) 
		extends CommandResult

case class AerospikeWriteListener[T <: Any]()
				(implicit converter: AerospikeValueConverter[T],
						  factory: Factory) 
				extends Listener[AerospikeWriteReturn[T]](factory) 
				with WriteListener {
  
  	def onSuccess(key: Key) = {
  	  promise.success(
  	      AerospikeWriteReturn(
  			  AerospikeKey(key)))
  	}
	
	def onFailure(exception: AerospikeException) = {
  	  promise.failure(exception)
	}
}

case class AerospikeDeleteListener[T <: Any]()
		(implicit converter: AerospikeValueConverter[T],
				  factory: Factory)
		extends Listener[AerospikeDeleteReturn[T]](factory) 
		with DeleteListener {
    def onSuccess(key: Key, existed: Boolean) = {
  	  promise.success(
  	      AerospikeDeleteReturn((
  			  AerospikeKey(key), existed)))
  	}
	
	def onFailure(exception: AerospikeException) = {
  	  promise.failure(exception)
	}
}

case class AerospikeExistsListener[T <: Any]()
		(implicit converter: AerospikeValueConverter[T],
				  factory: Factory)
		extends Listener[AerospikeDeleteReturn[T]](factory)
		with ExistsListener{
    def onSuccess(key: Key, existed: Boolean) = {
  	  promise.success(
  	      AerospikeDeleteReturn((
  			  AerospikeKey(key), existed)))
  	}
	
	def onFailure(exception: AerospikeException) = {
  	  promise.failure(exception)
	}
}

case class AerospikeReadListener[T <: Any]
			(converter: AerospikeRecordReader)
			(implicit
			    keyConverter: AerospikeValueConverter[_],
			    factory: Factory)
			extends Listener[AerospikeReadReturn[T]](factory)
			with RecordListener {
	implicit val conv = converter 
  
	def onSuccess(key: Key, record: Record) = {
	  if (record==null)
	    	promise.failure(new AerospikeException(s"Key not found: $key"))
	  else {
		try {
		  val ar =
			AerospikeRecord(record)
		  promise.success(
			AerospikeReadReturn(
  			  AerospikeKey(key), ar))
		} catch {
	    	case err: Throwable => 
	    	  //err.printStackTrace();
	    	  promise.failure(new AerospikeException(err))
		}
  	  
	  }
  	}
	
	def onFailure(exception: AerospikeException) = {
  	  promise.failure(exception)
	}
}

case class AerospikeMultipleReadListener[T <: Any]
			(converter: AerospikeRecordReader)
			(implicit
			    keyConverter: AerospikeValueConverter[_],
			    factory: Factory)
			extends Listener[AerospikeMultipleReadReturn[T]](factory)
			with RecordArrayListener {
	implicit val conv = converter 
  
	def onSuccess(keys: Array[Key], records: Array[Record]) = {
	  try {
		  val results = 
		    keys.zip(records).map(kr =>
		      	(AerospikeKey(kr._1), AerospikeRecord(kr._2))
		        )
		  implicit val readers = 
		    keys.map(_ => converter).toSeq
		  promise.success(
			AerospikeMultipleReadReturn(
  			  results))
		} catch {
	    	case err: Throwable => 
	    	  //err.printStackTrace();
	    	  promise.failure(new AerospikeException(err))
		}
  	}
	
	def onFailure(exception: AerospikeException) = {
  	  promise.failure(exception)
	}
}

case class AerospikeMultipleDifferentReadListener[T <: Any]
			(keys_converters: Seq[(AerospikeValueConverter[_],AerospikeRecordReader)])
			(implicit
			    factory: Factory)
			extends Listener[AerospikeMultipleReadReturn[T]](factory)
			with RecordArrayListener {
	def onSuccess(keys: Array[Key], records: Array[Record]) = {
	  try {
		  val results = 
		    for {
		      i_kr <- keys.zip(records).zipWithIndex
		      keyConverter = try {Some(keys_converters(i_kr._2)._1)} catch {case _: Throwable => None}
		      recordConverter = try {Some(keys_converters(i_kr._2)._2)} catch {case _: Throwable => None}
		      if (keyConverter.isDefined && recordConverter.isDefined)
		    } yield {
		      (AerospikeKey(i_kr._1._1)(keys_converters(i_kr._2)._1), 
		      AerospikeRecord(i_kr._1._2)(keys_converters(i_kr._2)._2))
		    }
		  implicit val readers = keys_converters.map(_._2)
		  promise.success(
			AerospikeMultipleReadReturn(
  			  results.toSeq))
		} catch {
	    	case err: Throwable => 
	    	  //err.printStackTrace();
	    	  promise.failure(new AerospikeException(err))
		}
  	}
	
	def onFailure(exception: AerospikeException) = {
  	  promise.failure(exception)
	}
}

case class AerospikeSequenceReadListener[T <: Any]
			(converter: AerospikeRecordReader)
			(implicit
			    keyConverter: AerospikeValueConverter[T],
			    factory: Factory)
			extends Listener[AerospikeMultipleReadReturn[T]](factory)
			with RecordSequenceListener {
	implicit val conv = converter 
	
	val stream: StreamBuilder[(AerospikeKey[T], AerospikeRecord)] = new StreamBuilder()
	
	def onRecord(key: Key, record: Record) = {
		val toAdd =
		  try 
			Some((AerospikeKey(key), AerospikeRecord(record)))
		  catch {
		  	case err : Throwable => err.printStackTrace();None
		  }
		
		toAdd.map(stream += _) 
    }
	
  	def onSuccess() = {
  	  	val result = stream.result.toSeq
  	  	val readers = 
  	  	  for (i <- 0.to(result.length)) yield converter
  	  	
		promise.success(
			AerospikeMultipleReadReturn(
  			  result)(readers,factory))
    }
  	
    def onFailure(exception: AerospikeException) = {
  	  promise.failure(exception)
	}
}