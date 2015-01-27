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

package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.{ Record, Value }
import scala.collection.JavaConverters._
import scala.collection.convert.WrapAsScala._
import AerospikeValue._

class AerospikeRecord(
    bins: Seq[AerospikeBin[_]],
    generation: Int,
    expiration: Int) {

  def getBins = bins

  def get[X](binName: String): AerospikeValue[X] = {
    getOpt[X](binName).getOrElse(
      throw new Exception(s"Bin name ${binName} not found"))
  }

  def getOpt[X](binName: String): Option[AerospikeValue[X]] = {
    bins.find(bin =>
      bin.name == binName
    ).map(bin =>
      bin.value match {
        case x: AerospikeValue[X] => Some(x)
        case _ => None
      }
    ).flatten
  }

  def toRecordBins: Seq[(String, Object)] =
    bins.map(_.toRecordValue)

  val inner =
    new Record(
      toRecordBins.toMap.asJava,
      generation,
      expiration
    )
}

case class AerospikeRecordReader(stub: Map[String, AerospikeValueConverter[_]]) {

  def getStub = stub

}

object AerospikeRecordReader {

  def apply(bins: Seq[AerospikeBin[_]]): AerospikeRecordReader =
    new AerospikeRecordReader(bins.map(bin => bin.name -> bin.converter).toMap)

}

object AerospikeRecord {

  def apply(record: Record)(implicit stub: AerospikeRecordReader): AerospikeRecord = {
    new AerospikeRecord(stub.getStub.map(bin =>
      AerospikeBin((bin._1, (record.bins.get(bin._1))), bin._2)).toSeq,
      record.generation,
      record.expiration
    )
  }

}