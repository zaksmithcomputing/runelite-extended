/*
 * Copyright (c) 2018 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.injector.raw;

import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.asm.ClassGroup;
import net.runelite.asm.Field;
import net.runelite.asm.Method;
import net.runelite.asm.attributes.code.Instruction;
import net.runelite.asm.attributes.code.Instructions;
import net.runelite.asm.attributes.code.Label;
import net.runelite.asm.attributes.code.instructions.ALoad;
import net.runelite.asm.attributes.code.instructions.GetField;
import net.runelite.asm.attributes.code.instructions.GetStatic;
import net.runelite.asm.attributes.code.instructions.Goto;
import net.runelite.asm.attributes.code.instructions.IAStore;
import net.runelite.asm.attributes.code.instructions.IfEq;
import net.runelite.asm.attributes.code.instructions.InvokeStatic;
import net.runelite.asm.attributes.code.instructions.InvokeVirtual;
import net.runelite.asm.attributes.code.instructions.LDC;
import net.runelite.asm.attributes.code.instructions.PutStatic;
import net.runelite.asm.execution.Execution;
import net.runelite.asm.execution.InstructionContext;
import net.runelite.asm.execution.MethodContext;
import net.runelite.injector.Inject;
import static net.runelite.injector.InjectUtil.findDeobField;
import static net.runelite.injector.InjectUtil.findMethod;
import static net.runelite.injector.InjectUtil.findObField;
import static net.runelite.injector.InjectUtil.findStaticMethod;
import net.runelite.injector.InjectionException;
import net.runelite.injector.MixinInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DrawUnderObjects
{
	private static final Logger log = LoggerFactory.getLogger(DrawUnderObjects.class);

	private final Inject inject;

	public DrawUnderObjects(Inject inject)
	{
		this.inject = inject;
	}

	public void inject() throws InjectionException
	{
		injectDrawUnderObjectsHooks();
	}

	private void injectDrawUnderObjectsHooks() throws InjectionException
	{
		String[] rasterFunctions = new String[]
			{
				"Rasterizer3D_texture",
				"Rasterizer3D_vert",
				"Rasterizer3D_textureAffine",
				"Rasterizer3D_horiz"
			};

		ClassGroup vanilla = inject.getVanilla();

		net.runelite.asm.pool.Method blendUnderObjectBuffer = findStaticMethod(vanilla, "blendUnderObjectBuffer").getPoolMethod();
		Field isUnderObject = findObField(inject, "isUnderObject");

		for (String rasterFn : rasterFunctions)
		{
			// Make a copy of the rasterXAlpha method and swap its stores into the pixel arrays to our blend function
			// We inject the if statement before the call to the function because it helps performance quite a bit.

			net.runelite.asm.Method fn = findMethod(inject, rasterFn);
			net.runelite.asm.Method alpha = findMethod(inject, rasterFn + "Alpha");
			Method tile = new Method(alpha.getClassFile(), "tile" + rasterFn + "Alpha", alpha.getDescriptor());
			MixinInjector.moveCode(tile, alpha.getCode());
			tile.setAccessFlags(alpha.getAccessFlags());
			alpha.getClassFile().addMethod(tile);

			{
				// Swap IAStores in the copied method to blendObjectUnderBuffer
				Execution e = new Execution(vanilla);
				e.addMethod(tile);
				e.noInvoke = true;
				AtomicReference<MethodContext> pcontext = new AtomicReference<>(null);
				e.addMethodContextVisitor(pcontext::set);
				e.run();
				MethodContext methodContext = pcontext.get();

				Instructions instrs = tile.getCode().getInstructions();

				int iastores = 0;
				for (InstructionContext instrCtx : methodContext.getInstructionContexts())
				{
					Instruction instr = instrCtx.getInstruction();
					log.info("{} - instr {} instanceof {}",  rasterFn + "Alpha", instr, instr.getClass());

					if (instr instanceof IAStore)
					{
						log.info("{} - instr {} instanceof {}",  rasterFn + "Alpha", instr, instr.getClass());

						Instruction pusher = instrCtx.getPops().get(2).getPushed().getInstruction();
						if (pusher instanceof ALoad)
						{
							log.info("{} - getVariableIndex {} pusher {} instanceof {}", rasterFn + "Alpha", ((ALoad) pusher).getVariableIndex(), instrCtx.getPops().get(2).getPushed().getInstruction(), pusher.getClass());
						}
						if (pusher instanceof ALoad && ((ALoad) pusher).getVariableIndex() == 0)
						{
							instrs.replace(instr, new InvokeStatic(instrs, blendUnderObjectBuffer));
							iastores++;
						}
					}
					log.info("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
				}

				if (iastores <= 0)
				{
					throw new InjectionException("Found no IAStores in " + rasterFn + "Alpha");
				}
			}

			{
				// Inject a if statement before the calls to the Alpha method so it will
				// call the copied method if isUnderObject is true
				Instructions instrs = fn.getCode().getInstructions();

				int ivss = 0;
				ListIterator<Instruction> instrIter = instrs.getInstructions().listIterator();
				while (instrIter.hasNext())
				{
					Instruction instr = instrIter.next();
					if (instr instanceof InvokeStatic)
					{
						InvokeStatic ivs = (InvokeStatic) instr;
						if (ivs.getMethod().equals(alpha.getPoolMethod()))
						{
							ivss++;
							Label lfalse = new Label(instrs);
							Label ldone = new Label(instrs);

							instrIter.remove();
							instrIter.add(new GetStatic(instrs, isUnderObject.getPoolField()));
							instrIter.add(new IfEq(instrs, lfalse));
							instrIter.add(new InvokeStatic(instrs, tile.getPoolMethod()));
							instrIter.add(new Goto(instrs, ldone));
							instrIter.add(lfalse);
							instrIter.add(new InvokeStatic(instrs, alpha.getPoolMethod()));
							instrIter.add(ldone);
						}
					}
				}
				if (ivss <= 0)
				{
					throw new InjectionException("Found no calls to " + rasterFn + "Alphaa in " + rasterFn);
				}
			}
		}

		// Inject sets for isUnderObject in Region::draw, so it true when we want
		// to be infront of what is being rendered, and false when we want to be behind it.

		net.runelite.asm.Method obmethod = findMethod(inject, "drawTile", "Scene");

		// Set to true before drawTile(under|overlay)
		net.runelite.asm.pool.Method drawTileOverlay = findMethod(inject, "drawTileOverlay").getPoolMethod();
		int drawTileOverlayI = 0;
		net.runelite.asm.pool.Method drawTileUnderlay = findMethod(inject, "drawTileUnderlay").getPoolMethod();
		int drawTileUnderlayI = 0;
		// False, so we don't draw over walls
		net.runelite.asm.pool.Field wallObject = findDeobField(inject, "boundaryObject", "Tile").getPoolField();
		int wallObjectI = 0;
		// true again, so the groundObject is drawn
		net.runelite.asm.pool.Field groundObject = findDeobField(inject, "floorDecoration", "Tile").getPoolField();
		int groundObjectI = 0;
		// off again, after the groundObject
		net.runelite.asm.pool.Field itemLayer = findDeobField(inject, "tileItemPile", "Tile").getPoolField();
		int itemLayerI = 0;

		Instructions instrs = obmethod.getCode().getInstructions();
		ListIterator<Instruction> instrIter = instrs.getInstructions().listIterator();
		while (instrIter.hasNext())
		{
			Instruction instr = instrIter.next();

			if (instr instanceof InvokeVirtual)
			{
				InvokeVirtual ivs = (InvokeVirtual) instr;
				boolean inject = false;
				if (ivs.getMethod().equals(drawTileOverlay))
				{
					inject = true;
					drawTileOverlayI++;
				}
				if (ivs.getMethod().equals(drawTileUnderlay))
				{
					inject = true;
					drawTileUnderlayI++;
				}
				if (inject)
				{
					instrIter.previous();
					instrIter.add(new LDC(instrs, 1));
					instrIter.add(new PutStatic(instrs, isUnderObject));
					instrIter.next();
				}
			}
			if (instr instanceof GetField)
			{
				GetField gf = (GetField) instr;
				if (gf.getField().equals(wallObject))
				{
					if (wallObjectI++ == 1)
					{
						instrIter.add(new LDC(instrs, 0));
						instrIter.add(new PutStatic(instrs, isUnderObject));
					}
				}
				if (gf.getField().equals(groundObject))
				{
					if (groundObjectI++ == 0)
					{
						instrIter.add(new LDC(instrs, 1));
						instrIter.add(new PutStatic(instrs, isUnderObject));
					}
				}
				if (gf.getField().equals(itemLayer))
				{
					if (itemLayerI++ == 0)
					{
						instrIter.add(new LDC(instrs, 0));
						instrIter.add(new PutStatic(instrs, isUnderObject));
					}
				}
			}
		}

		// Check we saw the right amount of things
		// If these start throwing it is probably OK to disable them, just check they still run in the correct order
		if (drawTileOverlayI != 2)
		{
			throw new InjectionException("Found wrong amount of drawTileOverlay: 2 != " + drawTileOverlayI);
		}
		else
		{
			log.info("DrawTileOverlay replaced {} method calls", drawTileOverlayI);
		}

		if (drawTileUnderlayI != 2)
		{
			throw new InjectionException("Found wrong amount of drawTileUnderlay: 2 != " + drawTileUnderlayI);
		}
		else
		{
			log.info("DrawTileUnderlay replaced {} method calls", drawTileUnderlayI);
		}

		if (wallObjectI != 4)
		{
			throw new InjectionException("Found wrong amount of wallObject: 4 != " + groundObjectI);
		}
		else
		{
			log.info("GroundObject replaced {} method calls", groundObjectI);
		}

		if (groundObjectI != 1)
		{
			throw new InjectionException("Found wrong amount of groundObject: 1 != " + groundObjectI);
		}
		else
		{
			log.info("GroundObject replaced {} method calls", groundObjectI);
		}

		if (itemLayerI != 2)
		{
			throw new InjectionException("Found wrong amount of itemLayer: 2 != " + itemLayerI);
		}
		else
		{
			log.info("ItemLayerIreplaced {} method calls", itemLayerI);
		}

	}
}