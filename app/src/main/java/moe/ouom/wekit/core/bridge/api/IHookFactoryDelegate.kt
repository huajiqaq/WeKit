package moe.ouom.wekit.core.bridge.api

import moe.ouom.wekit.core.model.BaseClickableFunctionHookItem
import moe.ouom.wekit.core.model.BaseHookItem
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem

/**
 * 定义 HookItemFactory 的所有能力
 */
interface IHookFactoryDelegate {
    fun getAllItemList(): List<BaseHookItem>
    fun getAllSwitchFunctionItemList(): List<BaseSwitchFunctionHookItem>
    fun getAllClickableFunctionItemList(): List<BaseClickableFunctionHookItem>
    fun findHookItemByPath(path: String): BaseSwitchFunctionHookItem?
}