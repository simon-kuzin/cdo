/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.net4j.channel;

import org.eclipse.net4j.ILocationAware;
import org.eclipse.net4j.buffer.IBuffer;
import org.eclipse.net4j.buffer.IBufferHandler;
import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.util.collection.Closeable;
import org.eclipse.net4j.util.event.INotifier;
import org.eclipse.net4j.util.security.IUserAware;

/**
 * A bidirectional communications channel for the asynchronous exchange of {@link IBuffer buffers}. A channel is
 * lightweight and virtual in the sense that it does not necessarily represent a single physical connection like a TCP
 * socket connection. The underlying physical connection is represented by a {@link IChannelMultiplexer channel
 * multiplexer}.
 * <p>
 * <dt><b>Class Diagram:</b></dt>
 * <dd><img src="doc-files/Channels.png" title="Diagram Buffers" border="0" usemap="#Channels.png"/></dd>
 * <p>
 * <MAP NAME="Channels.png"> <AREA SHAPE="RECT" COORDS="301,8,451,68" HREF="IChannelID.html"> <AREA SHAPE="RECT"
 * COORDS="301,141,451,211" HREF="IChannel.html"> <AREA SHAPE="RECT" COORDS="599,151,696,201"
 * HREF="IBufferHandler.html"> <AREA SHAPE="RECT" COORDS="7,151,96,201" HREF="IConnector.html"> </MAP>
 * <p>
 * <dt><b>Sequence Diagram: Communication Process</b></dt>
 * <dd><img src="doc-files/CommunicationProcess.jpg" title="Communication Process" border="0"
 * usemap="#CommunicationProcess.jpg"/></dd>
 * <p>
 * <MAP NAME="CommunicationProcess.jpg"> <AREA SHAPE="RECT" COORDS="128,94,247,123" HREF="IConnector.html"> <AREA
 * SHAPE="RECT" COORDS="648,95,767,123" HREF="IConnector.html"> <AREA SHAPE="RECT" COORDS="509,254,608,283"
 * HREF="IChannel.html"> <AREA SHAPE="RECT" COORDS="287,355,387,383" HREF="IChannel.html"> <AREA SHAPE="RECT"
 * COORDS="818,195,897,222" HREF="IProtocol.html"> </MAP>
 * <p>
 * An example for opening a channel on an {@link IConnector} and sending an {@link IBuffer}:
 * <p>
 * <pre style="background-color:#ffffc8; border-width:1px; border-style:solid; padding:.5em;"> // Open a channel
 * IChannel channel = connector.openChannel(); short channelID = channel.getIndex(); // Fill a buffer Buffer buffer =
 * bufferProvider.getBuffer(); ByteBuffer byteBuffer = buffer.startPutting(channelID); byteBuffer.putDouble(15.47); //
 * Let the channel send the buffer without blocking channel.sendBuffer(buffer); </pre>
 * <p>
 * An example for receiving {@link IBuffer}s from channels on an {@link IConnector}:
 * <p>
 * <pre style="background-color:#ffffc8; border-width:1px; border-style:solid; padding:.5em;"> // Create a receive
 * handler final IBufferHandler receiveHandler = new IBufferHandler() { public void handleBuffer(IBuffer buffer) {
 * ByteBuffer byteBuffer = buffer.getByteBuffer(); IOUtil.OUT().println(&quot;Received &quot; + byteBuffer.getDouble());
 * buffer.release(); } }; // Set the receive handler to all new channels connector.addListener(new
 * ContainerEventAdapter() { protected void onAdded(IContainer container, Object element) { IChannel channel =
 * (IChannel)element; channel.setReceiveHandler(receiveHandler); } }); </pre>
 * 
 * @author Eike Stepper
 * @noimplement This interface is <b>not</b> intended to be implemented by clients. Providers of channels (for example
 *              for new physical connection types) have to extend/subclass {@link org.eclipse.spi.net4j.InternalChannel
 *              InternalChannel}.
 */
public interface IChannel extends ILocationAware, IUserAware, IBufferHandler, INotifier, Closeable
{
  /**
   * Returns the ID of this channel. The ID is unique at any time among all channels of the associated
   * {@link IChannelMultiplexer multiplexer}.
   * 
   * @since 2.0
   */
  public short getID();

  /**
   * Returns the multiplexer this channel is associated with. This channel multiplexer can be used, for example, to open
   * additional channels that will be multiplexed through the same transport medium.
   * 
   * @since 2.0
   */
  public IChannelMultiplexer getMultiplexer();

  /**
   * Asynchronously sends the given buffer to the receive handler of the peer channel.
   */
  public void sendBuffer(IBuffer buffer);

  /**
   * Returns the <code>IBufferHandler</code> that handles buffers received from the peer channel.
   */
  public IBufferHandler getReceiveHandler();

  /**
   * Sets the <code>IBufferHandler</code> to handle buffers received from the peer channel.
   */
  public void setReceiveHandler(IBufferHandler receiveHandler);
}
