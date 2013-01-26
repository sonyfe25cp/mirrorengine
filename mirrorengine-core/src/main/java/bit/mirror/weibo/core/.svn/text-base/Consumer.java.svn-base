package bit.mirror.weibo.core;

/**
 * 消费者接口，专门消费数据
 * @author lins
 * @date 2012-6-19
 **/
public interface Consumer<T>{
	public void setProducer(Producer<T> p);
	public Producer<T> getProducer();
	public void consume(Producer<T> p) throws Exception;
	public void consume() throws Exception;
}
