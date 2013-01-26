package bit.mirror.weibo.core;

/**
 * weibo爬虫里面的processor，继承了生产者和消费者接口
 * Processor<T, Z> which produces T and consumes Z.
 * @author lins
 * @date 2012-6-19
 **/
public interface Processor<T, Z> extends Producer<T>, Consumer<Z>, Runnable {
	public Manager getManager();
	public void setManager(Manager manager);
}
